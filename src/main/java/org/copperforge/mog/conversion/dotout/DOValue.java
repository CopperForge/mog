package org.copperforge.mog.conversion.dotout;

import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DOValue<T> implements Comparable<DOValue<?>> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final DOField field;

    private final byte[] bytes;

    protected T value = null;

    private boolean nullValue  = false;

    public DOValue(DOField field, byte[] bytes) {
        this.field = field;
        this.bytes = bytes;
    }

    public DOField getField() {
        return field;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public T value() {
        return value;
    }

    public String hex() {
        return Hex.encodeHexString(this.getBytes());
    }

    @Override
    public String toString() {
        if (value == null) return null;
        if (field != null) return field.toString() + " = '" + value + "', 0x" + Hex.encodeHexString(bytes);
        else return value.getClass().getName() + " = '" + value + "', 0x" + Hex.encodeHexString(bytes);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((field == null) ? 0 : field.hashCode());
        result = prime * result + Arrays.hashCode(bytes);
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
        DOValue<?> other = (DOValue<?>) obj;
        if (field == null) {
            if (other.field != null)
                return false;
        } else if (!field.equals(other.field))
            return false;
        if (!Arrays.equals(bytes, other.bytes))
            return false;
        return true;
    }

    @Override
    public int compareTo(DOValue<?> o) {
        return this.value().toString().compareTo(o.value().toString());
    }

    public Logger getLog() {
        return log;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public boolean isNullValue() {
        return nullValue;
    }

    public void setNullValue(boolean nullValue) {
        this.nullValue = nullValue;
    }

    
}
