package org.copperforge.mog.reporting.core;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Reportable {

    private final Map<String, Object> fields = new HashMap<>();

    public Reportable() {

    }

    public Reportable(Map<String, Object> fields) {
        this.fields.clear();
        this.fields.putAll(fields);
    }

    public Reportable(Object obj) throws ReportException {
        Field[] objFields = obj.getClass().getFields();
        for (Field objField : objFields) {
            try {
                set(objField.getName(), objField.get(obj));
            } catch (Exception e) {
                throw new ReportException(e);
            }
        }
    }

    public Object get(String key) {
        return get(fields, key);
    }

    public Map<String, Object> fields() {
        return fields;
    }

    public Set<String> keys() {
        return fields.keySet();
    }

    public boolean containsKey(String key) {
        return fields.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    protected Object get(Map<String, Object> reportable, String key) {
        if (key.contains(".")) {
            // nested key, get the base
            int iend = key.indexOf(".");
            Object top = reportable.get(key.substring(0, iend));
            String childKey = key.substring(iend+1);

            // test object is Map<String, Object>
            if (top instanceof Map) return get((Map<String, Object>) top, childKey);
            else if (top instanceof Reportable) return get(((Reportable) top).fields(), key);
            else return top.toString();
        }

        return reportable.get(key);
    }

    public String asString(String key) {
        Object value = get(key);
        return (value == null) ? null : value.toString();
    }

    public void set(String key, Object value) {
        if (fields.containsKey(key)) fields.remove(key);
        fields.put(key, value);
    }

}
