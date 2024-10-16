package org.greenplum.pxf.plugins.hdfs.parquet;

import org.apache.parquet.filter2.compat.FilterCompat;
import org.apache.parquet.filter2.predicate.FilterApi;
import org.apache.parquet.filter2.predicate.FilterPredicate;
import org.apache.parquet.filter2.predicate.Operators;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;
import org.greenplum.pxf.api.filter.*;
import org.greenplum.pxf.api.utilities.ColumnDescriptor;
import org.greenplum.pxf.plugins.hdfs.parquet.converters.ParquetTypeConverter;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.apache.parquet.filter2.predicate.FilterApi.*;
import static org.greenplum.pxf.plugins.hdfs.parquet.converters.ParquetTypeConverter.FILTER_COLUMN;

/**
 * This is the implementation of {@link TreeVisitor} for Parquet.
 * <p>
 * The class visits all the {@link Node}s from the expression tree,
 * and builds a simple (single {@link FilterCompat.Filter} class) for
 * {@link org.greenplum.pxf.plugins.hdfs.ParquetFileAccessor} to use for its
 * scan.
 */
public class ParquetRecordFilterBuilder implements TreeVisitor {
    private final Map<String, Type> fields;
    private final List<ColumnDescriptor> columnDescriptors;
    private final Deque<FilterPredicate> filterQueue;
    private final ParquetTypeConverterFactory parquetTypeConverterFactory;

    /**
     * Constructor
     *
     * @param columnDescriptors the list of column descriptors
     * @param originalFields    a map of field names to types
     */
    public ParquetRecordFilterBuilder(List<ColumnDescriptor> columnDescriptors, Map<String, Type> originalFields,
                                      ParquetTypeConverterFactory parquetTypeConverterFactory) {
        this.columnDescriptors = columnDescriptors;
        this.filterQueue = new LinkedList<>();
        this.fields = originalFields;
        this.parquetTypeConverterFactory = parquetTypeConverterFactory;
    }

    @Override
    public Node before(Node node, int level) {
        return node;
    }

    @Override
    public Node visit(Node node, int level) {
        if (node instanceof OperatorNode) {
            OperatorNode operatorNode = (OperatorNode) node;
            Operator operator = operatorNode.getOperator();

            if (!operator.isLogical()) {
                processSimpleColumnOperator(operatorNode);
            }
        }
        return node;
    }

    @Override
    public Node after(Node node, int level) {
        if (node instanceof OperatorNode) {
            OperatorNode operatorNode = (OperatorNode) node;
            Operator operator = operatorNode.getOperator();
            if (operator.isLogical()) {
                processLogicalOperator(operator);
            }
        }
        return node;
    }

    /**
     * Returns the built record filter
     *
     * @return the built record filter
     */
    public FilterCompat.Filter getRecordFilter() {
        FilterPredicate predicate = filterQueue.poll();
        if (!filterQueue.isEmpty()) {
            throw new IllegalStateException("Filter queue is not empty after visiting all nodes");
        }
        return predicate != null ? FilterCompat.get(predicate) : FilterCompat.NOOP;
    }

    private void processLogicalOperator(Operator operator) {
        FilterPredicate right = filterQueue.poll();
        FilterPredicate left = null;

        if (right == null) {
            throw new IllegalStateException("Unable to process logical operator " + operator.toString());
        }

        if (operator == Operator.AND || operator == Operator.OR) {
            left = filterQueue.poll();

            if (left == null) {
                throw new IllegalStateException("Unable to process logical operator " + operator);
            }
        }

        switch (operator) {
            case AND:
                filterQueue.push(and(left, right));
                break;
            case OR:
                filterQueue.push(or(left, right));
                break;
            case NOT:
                filterQueue.push(not(right));
                break;
        }
    }

    /**
     * Handles simple column-operator-constant expressions.
     *
     * @param operatorNode the operator node
     */
    private void processSimpleColumnOperator(OperatorNode operatorNode) {

        Operator operator = operatorNode.getOperator();
        ColumnIndexOperandNode columnIndexOperand = operatorNode.getColumnIndexOperand();
        OperandNode valueOperand = null;

        if (operator != Operator.IS_NULL && operator != Operator.IS_NOT_NULL) {
            valueOperand = operatorNode.getValueOperand();
            if (valueOperand == null) {
                throw new IllegalArgumentException(
                        String.format("Operator %s does not contain an operand", operator));
            }
        }

        ColumnDescriptor columnDescriptor = columnDescriptors.get(columnIndexOperand.index());
        String filterColumnName = columnDescriptor.columnName();
        Type type = fields.get(filterColumnName);
        PrimitiveType.PrimitiveTypeName primitiveTypeName = type.asPrimitiveType().getPrimitiveTypeName();
        ParquetTypeConverter typeConverter = parquetTypeConverterFactory.create(type, columnDescriptor.getDataType());
        if (valueOperand != null && valueOperand.getDataType().isArrayType()) {
            throw new UnsupportedOperationException("Currently not supported IN on this level(should be rewritten before this builder).");
        }
        Object filterValue;
        try {
            // todo also look at data type of the filter value
            filterValue = (valueOperand != null) ? typeConverter.filterValue(valueOperand.toString()) : null;
        } catch (Exception ex) {
            if (ex.getMessage().contains(FILTER_COLUMN)) {
                throw new IllegalArgumentException(ex.getMessage().replace(FILTER_COLUMN, filterColumnName), ex);
            } else {
                throw ex;
            }
        }

        // INT96 cannot be pushed down
        // for more details look at org.apache.parquet.filter2.dictionarylevel.DictionaryFilter#expandDictionary
        // where INT96 are not dictionary values
        FilterPredicate simpleFilter;
        switch (primitiveTypeName) {
            case INT32:
                simpleFilter = ParquetRecordFilterBuilder.<Integer, Operators.IntColumn>getOperatorWithLtGtSupport(operator)
                        .apply(intColumn(type.getName()), (Integer) filterValue);
                break;

            case INT64:
                simpleFilter = ParquetRecordFilterBuilder.<Long, Operators.LongColumn>getOperatorWithLtGtSupport(operator)
                        .apply(longColumn(type.getName()), (Long) filterValue);
                break;

            case FIXED_LEN_BYTE_ARRAY:
            case BINARY:
                simpleFilter = ParquetRecordFilterBuilder.<Binary, Operators.BinaryColumn>getOperatorWithLtGtSupport(operator)
                        .apply(binaryColumn(type.getName()), (Binary) filterValue);
                break;

            case BOOLEAN:
                // Boolean does not SupportsLtGt
                simpleFilter = ParquetRecordFilterBuilder.<Boolean, Operators.BooleanColumn>getOperatorWithEqNotEqSupport(operator)
                        .apply(booleanColumn(type.getName()), (Boolean) filterValue);
                break;

            case FLOAT:
                simpleFilter = ParquetRecordFilterBuilder.<Float, Operators.FloatColumn>getOperatorWithLtGtSupport(operator)
                        .apply(floatColumn(type.getName()), (Float) filterValue);
                break;

            case DOUBLE:
                simpleFilter = ParquetRecordFilterBuilder.<Double, Operators.DoubleColumn>getOperatorWithLtGtSupport(operator)
                        .apply(doubleColumn(type.getName()), (Double) filterValue);
                break;

            default:
                throw new UnsupportedOperationException(String.format("Column %s of type %s is not supported",
                        type.getName(), type.asPrimitiveType().getPrimitiveTypeName()));
        }

        filterQueue.push(simpleFilter);
    }

    /**
     * Returns the FilterPredicate function that supports equals and not equals
     * for the given operator
     *
     * @param operator the operator
     * @param <T>      the type
     * @param <C>      the column type
     * @return the FilterPredicate function
     */
    private static <T extends Comparable<T>, C extends Operators.Column<T> & Operators.SupportsEqNotEq> BiFunction<C, T, FilterPredicate> getOperatorWithEqNotEqSupport(Operator operator) {
        switch (operator) {
            case IS_NULL:
            case EQUALS:
            case NOOP:
                return FilterApi::eq;
            // NOT boolean wraps a NOOP
            //       NOT
            //        |
            //       NOOP
            //        |
            //    ---------
            //   |         |
            //   4        true
            // that needs to be replaced with equals
            case IS_NOT_NULL:
            case NOT_EQUALS:
                return FilterApi::notEq;

            default:
                throw new UnsupportedOperationException("not supported " + operator);
        }
    }

    /**
     * Returns the FilterPredicate function that supports less than /
     * greater than for the given operator
     *
     * @param operator the operator
     * @param <T>      the type
     * @param <C>      the column type
     * @return the FilterPredicate function
     */
    private static <T extends Comparable<T>, C extends Operators.Column<T> & Operators.SupportsLtGt> BiFunction<C, T, FilterPredicate> getOperatorWithLtGtSupport(Operator operator) {

        switch (operator) {
            case LESS_THAN:
                return FilterApi::lt;
            case GREATER_THAN:
                return FilterApi::gt;
            case LESS_THAN_OR_EQUAL:
                return FilterApi::ltEq;
            case GREATER_THAN_OR_EQUAL:
                return FilterApi::gtEq;
            default:
                return getOperatorWithEqNotEqSupport(operator);
        }
    }

}
