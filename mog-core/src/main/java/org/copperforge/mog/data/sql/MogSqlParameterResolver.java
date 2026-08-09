package org.copperforge.mog.data.sql;

import java.math.BigDecimal;
import java.sql.JDBCType;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.copperforge.mog.MogException;
import org.copperforge.mog.data.filter.MogQueryParameter;
import org.copperforge.mog.runtime.MogContext;
import org.copperforge.mog.var.MogVariableService;

public class MogSqlParameterResolver {

    private static final Pattern EXACT_PLACEHOLDER = Pattern.compile("^\\$\\{([A-Za-z0-9_]+)\\}$");

    public List<MogResolvedSqlParameter> resolve(MogPreparedSql preparedSql,
            Map<String, MogQueryParameter> definitions, MogContext context) throws MogException {
        List<MogResolvedSqlParameter> resolved = new ArrayList<>();
        Map<String, MogQueryParameter> parameterDefinitions = definitions != null ? definitions : Map.of();

        for (String name : preparedSql.parameterNames()) {
            if (!parameterDefinitions.containsKey(name)) {
                throw new MogException("Missing SQL parameter definition for '" + name + "'");
            }
            MogQueryParameter parameter = parameterDefinitions.get(name);
            if (parameter == null) {
                throw new MogException("SQL parameter definition for '" + name + "' must not be null");
            }
            JDBCType jdbcType = resolveJdbcType(name, parameter.getJdbcType());
            Object value = resolveValue(name, parameter.getValue(), context);
            validateSupportedValue(name, value);
            resolved.add(new MogResolvedSqlParameter(name, value, jdbcType));
        }

        return List.copyOf(resolved);
    }

    private JDBCType resolveJdbcType(String name, String rawJdbcType) throws MogException {
        if (rawJdbcType == null || rawJdbcType.isBlank()) {
            return null;
        }
        try {
            return JDBCType.valueOf(rawJdbcType.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new MogException("Unsupported jdbcType '" + rawJdbcType + "' for SQL parameter '" + name + "'", e);
        }
    }

    private Object resolveValue(String parameterName, Object rawValue, MogContext context) throws MogException {
        if (rawValue == null) {
            return null;
        }
        if (!(rawValue instanceof String rawString)) {
            return rawValue;
        }

        Matcher exact = EXACT_PLACEHOLDER.matcher(rawString);
        if (exact.matches()) {
            return resolveVariable(parameterName, exact.group(1), context);
        }

        MogVariableService variables = new MogVariableService(context);
        for (String variable : variables.findVariables(rawString)) {
            resolveVariable(parameterName, variable, context);
        }
        return variables.envsubst(rawString);
    }

    private Object resolveVariable(String parameterName, String variableName, MogContext context) throws MogException {
        if (context != null && context.getVariables() != null && context.getVariables().containsKey(variableName)) {
            Object value = context.getVariables().get(variableName);
            if (value == null) {
                throw new MogException("SQL parameter '" + parameterName + "' references unresolved variable '"
                        + variableName + "'");
            }
            return value;
        }

        String value = new MogVariableService(context).get(variableName);
        if (value == null || value.isEmpty()) {
            throw new MogException("SQL parameter '" + parameterName + "' references unresolved variable '"
                    + variableName + "'");
        }
        return value;
    }

    private void validateSupportedValue(String name, Object value) throws MogException {
        if (value == null
                || value instanceof String
                || value instanceof Integer
                || value instanceof Long
                || value instanceof BigDecimal
                || value instanceof Boolean
                || value instanceof LocalDate
                || value instanceof LocalDateTime
                || value instanceof java.sql.Date
                || value instanceof Timestamp) {
            return;
        }
        throw new MogException("Unsupported value type '" + value.getClass().getName()
                + "' for SQL parameter '" + name + "'");
    }
}
