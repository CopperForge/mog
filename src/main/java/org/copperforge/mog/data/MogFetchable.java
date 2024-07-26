package org.copperforge.mog.data;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.copperforge.mog.MogException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MogFetchable {

    private final Logger log = LoggerFactory.getLogger(MogFetchable.class);
    protected final Map<String, Object> fields = new HashMap<>();
    private final String arrayPatternText = "(.+)\\[(\\d?)\\]\\.?(.*)";
    private final Pattern arrayPattern;

    public MogFetchable() {
        arrayPattern = Pattern.compile(arrayPatternText);
    }

    public MogFetchable(Map<String, Object> fields) {
        arrayPattern = Pattern.compile(arrayPatternText);
        this.fields.clear();
        this.fields.putAll(fields);
    }

    public MogFetchable(Object obj) throws MogException {
        arrayPattern = Pattern.compile(arrayPatternText);
        Field[] objFields = obj.getClass().getFields();
        for (Field objField : objFields) {
            try {
                set(objField.getName(), objField.get(obj));
            } catch (Exception e) {
                throw new MogException(e);
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
        log.debug("Getting key '" + key + "' from reportable");
        if (key.contains(".")) {
            // nested key, get the base
            int iend = key.indexOf(".");
            String topKey = key.substring(0, iend);

            Matcher matcher = arrayPattern.matcher(topKey);
            boolean isArray = matcher.matches();
            int index = -1; // assume a join unless index is specified
            if (isArray) {
                // we have an array, handle accordingly
                topKey = matcher.group(1);
                log.debug("new topKey = " + topKey);
                if (!matcher.group(2).isEmpty())
                    index = Integer.parseInt(matcher.group(2));
                log.debug("index = " + index);
            }

            Object top = reportable.get(topKey);
            log.debug("Got top of " + top);
            String childKey = key.substring(iend + 1);

            // test object is Map<String, Object>
            if (top instanceof List<?> && isArray) {
                List<?> topColl = (List<?>) top;
                if (index == -1) {
                    String result = "";
                    for (Object t : topColl) {
                        if (t instanceof Map)
                            result += get((Map<String, Object>) t, childKey) + ", ";
                        else if (t instanceof MogFetchable)
                            result += get(((MogFetchable) t).fields(), childKey) + ", ";
                        else
                            result += t.toString() + ", ";
                    }
                    log.debug("Result = " + result);
                    return result;
                } else {
                    top = topColl.get(index);
                }
            }

            if (top == null) 
                return "";
            else if (top instanceof Map)
                return get((Map<String, Object>) top, childKey);
            else if (top instanceof MogFetchable)
                return get(((MogFetchable) top).fields(), key);
            else
                return top.toString();
        }

        return reportable.get(key);
    }

    public String asString(String key) {
        Object value = get(key);
        return (value == null) ? null : value.toString();
    }

    public void set(String key, Object value) {
        if (fields.containsKey(key))
            fields.remove(key);
        fields.put(key, value);
    }

    @Override
    public String toString() {
        return "MogFetchable [fields=" + fields + "]";
    }

}
