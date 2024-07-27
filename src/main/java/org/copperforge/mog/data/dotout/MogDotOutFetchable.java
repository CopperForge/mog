package org.copperforge.mog.data.dotout;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.copperforge.mog.MogException;
import org.copperforge.mog.data.MogFetchable;

import com.gwt.conversion.dotout.DOValue;

public class MogDotOutFetchable extends MogFetchable {

    public MogDotOutFetchable(Map<String, DOValue<?>> fields) throws MogException {
        super(fields);
        this.fields.putAll(fields);
    }

    public List<String> fieldNames() {
        return fields.keySet().stream().collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "MogDotOutFetchable [fields=" + fields() + "]";
    }

    public DOValue<?> value(String key) {
        return (DOValue<?>) fields.get(key);
    }

    @Override
    public Object get(String key) {
        return value(key) != null ? value(key).value() : null;
    }
    
}
