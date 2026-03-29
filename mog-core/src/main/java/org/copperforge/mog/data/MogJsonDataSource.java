package org.copperforge.mog.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.copperforge.mog.MogException;
import org.copperforge.mog.data.filter.MogDataFilter;
import org.copperforge.mog.data.filter.MogJsonFilter;
import org.copperforge.mog.runtime.MogContext;
import org.copperforge.mog.var.MogVariableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public class MogJsonDataSource extends MogFileDataSource {

    private final Logger log = LoggerFactory.getLogger(MogJsonDataSource.class);

    @Override
    public List<? extends MogFetchable> fetch(MogDataFilter filter, MogContext context) throws MogException {
        log.debug("filter = " + filter);
        return fetch(json((MogJsonFilter) filter), (MogJsonFilter) filter);
    }

    protected String json(MogJsonFilter filter) throws MogException {
        try {
            String pathStr = new MogVariableService().envsubst(getFile());
            Path path = Path.of(pathStr);
            return Files.readString(path, StandardCharsets.UTF_8);
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
