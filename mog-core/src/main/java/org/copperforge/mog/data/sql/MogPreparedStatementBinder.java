package org.copperforge.mog.data.sql;

import java.math.BigDecimal;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.copperforge.mog.MogException;

public class MogPreparedStatementBinder {

    public void bind(PreparedStatement statement, List<MogResolvedSqlParameter> parameters) throws MogException {
        if (statement == null) {
            throw new MogException("PreparedStatement must not be null");
        }
        List<MogResolvedSqlParameter> resolved = parameters != null ? parameters : List.of();
        for (int i = 0; i < resolved.size(); i++) {
            bind(statement, i + 1, resolved.get(i));
        }
    }

    private void bind(PreparedStatement statement, int index, MogResolvedSqlParameter parameter)
            throws MogException {
        Object value = parameter.value();
        JDBCType jdbcType = parameter.jdbcType();

        try {
            if (value == null) {
                // Types.NULL is a portable fallback only when the DSL does not provide jdbcType;
                // some drivers may require an explicit type for null parameters.
                int sqlType = jdbcType != null ? jdbcType.getVendorTypeNumber() : Types.NULL;
                statement.setNull(index, sqlType);
            } else if (value instanceof String stringValue) {
                statement.setString(index, stringValue);
            } else if (value instanceof Integer intValue) {
                statement.setInt(index, intValue);
            } else if (value instanceof Long longValue) {
                statement.setLong(index, longValue);
            } else if (value instanceof BigDecimal decimalValue) {
                statement.setBigDecimal(index, decimalValue);
            } else if (value instanceof Boolean booleanValue) {
                statement.setBoolean(index, booleanValue);
            } else if (value instanceof LocalDate localDate) {
                statement.setDate(index, java.sql.Date.valueOf(localDate));
            } else if (value instanceof LocalDateTime localDateTime) {
                statement.setTimestamp(index, Timestamp.valueOf(localDateTime));
            } else if (value instanceof java.sql.Date dateValue) {
                statement.setDate(index, dateValue);
            } else if (value instanceof Timestamp timestampValue) {
                statement.setTimestamp(index, timestampValue);
            } else {
                throw new MogException("Unsupported value type '" + value.getClass().getName()
                        + "' for SQL parameter '" + parameter.name() + "'");
            }
        } catch (SQLException e) {
            throw new MogException("Unable to bind SQL parameter '" + parameter.name() + "' at index " + index, e);
        }
    }
}
