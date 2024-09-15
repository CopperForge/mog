package org.copperforge.mog.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.copperforge.mog.MogException;
import org.copperforge.mog.data.filter.MogDataFilter;
import org.copperforge.mog.data.filter.MogJsonFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public class MogJsonDataSource extends MogFileDataSource {

    private final Logger log = LoggerFactory.getLogger(MogJsonDataSource.class);

    @Override
    public List<? extends MogFetchable> fetch(MogDataFilter filter) throws MogException {
        log.debug("filter = " + filter);
        return fetch(json((MogJsonFilter) filter), (MogJsonFilter) filter);
    }

    protected String json(MogJsonFilter filter) throws MogException {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(getFile()));
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            String ls = System.getProperty("line.separator");
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
            // delete the last new line separator
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            reader.close();
            return stringBuilder.toString();
        } catch (IOException e) {
            throw new MogException(e);
        }

    }

    protected List<? extends MogFetchable> fetch(String json, MogJsonFilter filter) throws MogException {
        DocumentContext jsonContext = JsonPath.parse(json);
        JsonPath path = JsonPath.compile(filter.getJsonPath());
        if (path.isDefinite()) {
            Object value = jsonContext.read(path);
            log.debug("value = " + value.getClass().getCanonicalName());
            return Arrays.asList(new MogFetchable(value));
        } else {
            List<Map<String, Object>> values = jsonContext.read(path);
            log.debug("values = " + values);
            return values.stream().map(MogFetchable::new).collect(Collectors.toList());
        }
    }

}
