package org.copperforge.mog.data.sql;

import java.sql.JDBCType;

public class MogResolvedSqlParameter {

    private final String name;
    private final Object value;
    private final JDBCType jdbcType;

    public MogResolvedSqlParameter(String name, Object value, JDBCType jdbcType) {
        this.name = name;
        this.value = value;
        this.jdbcType = jdbcType;
    }

    public String getName() {
        return name;
    }

    public String name() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public Object value() {
        return value;
    }

    public JDBCType getJdbcType() {
        return jdbcType;
    }

    public JDBCType jdbcType() {
        return jdbcType;
    }

    @Override
    public String toString() {
        return "MogResolvedSqlParameter [name=" + name + ", value=*, jdbcType=" + jdbcType + "]";
    }
}
