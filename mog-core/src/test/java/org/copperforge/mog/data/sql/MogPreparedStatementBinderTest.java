package org.copperforge.mog.data.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class MogPreparedStatementBinderTest {

    private final MogPreparedStatementBinder binder = new MogPreparedStatementBinder();

    @Test
    void bind_supportsPracticalJavaJdbcTypes() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:mog-binder-types;DB_CLOSE_DELAY=-1");
                var statement = connection.prepareStatement(
                        "select ? as s, ? as i, ? as l, ? as d, ? as b, ? as ld, ? as ldt, ? as sd, ? as ts")) {

            LocalDate localDate = LocalDate.of(2026, 8, 9);
            LocalDateTime localDateTime = LocalDateTime.of(2026, 8, 9, 16, 45, 30);
            java.sql.Date sqlDate = java.sql.Date.valueOf(LocalDate.of(2026, 8, 10));
            Timestamp timestamp = Timestamp.valueOf(LocalDateTime.of(2026, 8, 11, 12, 0, 0));

            binder.bind(statement, List.of(
                    param("s", "alpha"),
                    param("i", Integer.valueOf(7)),
                    param("l", Long.valueOf(8L)),
                    param("d", new BigDecimal("9.50")),
                    param("b", Boolean.TRUE),
                    param("ld", localDate),
                    param("ldt", localDateTime),
                    param("sd", sqlDate),
                    param("ts", timestamp)));

            try (var rs = statement.executeQuery()) {
                rs.next();
                assertEquals("alpha", rs.getString("s"));
                assertEquals(7, rs.getInt("i"));
                assertEquals(8L, rs.getLong("l"));
                assertEquals(new BigDecimal("9.50"), rs.getBigDecimal("d"));
                assertEquals(true, rs.getBoolean("b"));
                assertEquals(java.sql.Date.valueOf(localDate), rs.getDate("ld"));
                assertEquals(Timestamp.valueOf(localDateTime), rs.getTimestamp("ldt"));
                assertEquals(sqlDate, rs.getDate("sd"));
                assertEquals(timestamp, rs.getTimestamp("ts"));
            }
        }
    }

    @Test
    void bind_supportsTypedNull() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:mog-binder-null;DB_CLOSE_DELAY=-1");
                var statement = connection.prepareStatement("select ? as null_value")) {

            binder.bind(statement, List.of(new MogResolvedSqlParameter("value", null, JDBCType.BIGINT)));

            try (var rs = statement.executeQuery()) {
                rs.next();
                assertNull(rs.getObject("null_value"));
            }
        }
    }

    @Test
    void bind_supportsUntypedNullFallback() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:mog-binder-untyped-null;DB_CLOSE_DELAY=-1");
                var statement = connection.prepareStatement("select ? as null_value")) {

            binder.bind(statement, List.of(new MogResolvedSqlParameter("value", null, null)));

            try (var rs = statement.executeQuery()) {
                rs.next();
                assertNull(rs.getObject("null_value"));
            }
        }
    }

    @Test
    void bind_preservesRepeatedParameterOrder() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:mog-binder-order;DB_CLOSE_DELAY=-1");
                var statement = connection.prepareStatement("select ? as first_value, ? as second_value, ? as third_value")) {

            binder.bind(statement, List.of(
                    param("cycleId", Long.valueOf(10L)),
                    param("afterId", Long.valueOf(20L)),
                    param("cycleId", Long.valueOf(10L))));

            try (var rs = statement.executeQuery()) {
                rs.next();
                assertEquals(10L, rs.getLong("first_value"));
                assertEquals(20L, rs.getLong("second_value"));
                assertEquals(10L, rs.getLong("third_value"));
            }
        }
    }

    private MogResolvedSqlParameter param(String name, Object value) {
        return new MogResolvedSqlParameter(name, value, null);
    }
}
