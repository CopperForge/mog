package org.copperforge.mog.data.sql;

import java.util.List;

public class MogPreparedSql {

    private final String sql;
    private final List<String> parameterNames;

    MogPreparedSql(String sql, List<String> parameterNames) {
        this.sql = sql;
        this.parameterNames = List.copyOf(parameterNames);
    }

    public String getSql() {
        return sql;
    }

    public String sql() {
        return sql;
    }

    public List<String> getParameterNames() {
        return parameterNames;
    }

    public List<String> parameterNames() {
        return parameterNames;
    }

    @Override
    public String toString() {
        return "MogPreparedSql [sql=" + sql + ", parameterNames=" + parameterNames + "]";
    }
}
