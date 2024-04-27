package org.greenplum.pxf.plugins.hdfs.parquet.converters;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;
import org.greenplum.pxf.api.io.DataType;

public class BooleanParquetTypeConverter implements ParquetTypeConverter {

    @Override
    public DataType getDataType(Type type) {
        return DataType.BOOLEAN;
    }

    @Override
    public Object getValue(Group group, int columnIndex, int repeatIndex, Type type) {
        return group.getBoolean(columnIndex, repeatIndex);
    }

    @Override
    public void addValueToJsonArray(Group group, int columnIndex, int repeatIndex, Type type, ArrayNode jsonNode) {
        jsonNode.add(group.getBoolean(columnIndex, repeatIndex));
    }

    @Override
    public String getValueFromList(Group group, int columnIndex, int repeatIndex, PrimitiveType primitiveType) {
        return String.valueOf(getValue(group, columnIndex, repeatIndex, primitiveType));
    }
}
