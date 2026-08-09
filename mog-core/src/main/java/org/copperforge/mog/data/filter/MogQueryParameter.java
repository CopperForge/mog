package org.copperforge.mog.data.filter;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MogQueryParameter {

    private Object value;
    private String jdbcType;

    public MogQueryParameter() {
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public MogQueryParameter(Object value) {
        if (value instanceof Map<?, ?> values) {
            this.value = values.get("value");
            Object rawJdbcType = values.get("jdbcType");
            this.jdbcType = rawJdbcType != null ? rawJdbcType.toString() : null;
        } else {
            this.value = value;
        }
    }

    public MogQueryParameter(Object value, String jdbcType) {
        this.value = value;
        this.jdbcType = jdbcType;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getJdbcType() {
        return jdbcType;
    }

    public void setJdbcType(String jdbcType) {
        this.jdbcType = jdbcType;
    }

    @Override
    public String toString() {
        return "MogQueryParameter [value=*, jdbcType=" + jdbcType + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((jdbcType == null) ? 0 : jdbcType.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MogQueryParameter other = (MogQueryParameter) obj;
        if (jdbcType == null) {
            if (other.jdbcType != null)
                return false;
        } else if (!jdbcType.equals(other.jdbcType))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }
}
