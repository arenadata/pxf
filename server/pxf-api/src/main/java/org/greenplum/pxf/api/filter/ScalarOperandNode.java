package org.greenplum.pxf.api.filter;

import lombok.Getter;
import org.greenplum.pxf.api.io.DataType;

import static java.util.Objects.requireNonNull;

/**
 * Represents a scalar value (String, Long, Int).
 */
@Getter
public class ScalarOperandNode extends OperandNode {
    private final String value;

    /**
     * Constructs an ScalarOperandNode with the datum data type and value
     *
     * @param dataType the data type
     * @param value    the value
     */
    public ScalarOperandNode(DataType dataType, String value) {
        super(dataType);
        this.value = requireNonNull(value, "value is null");
    }

    @Override
    public String toString() {
        return value;
    }
}
