package org.copperforge.mog.http;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;

import org.copperforge.mog.MogBean;
import org.copperforge.mog.MogException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public class MogHttpResponse<T extends MogBean> {

    private final static Logger log = LoggerFactory.getLogger(MogHttpResponse.class);
    private final HttpResponse<String> httpResponse;

    private final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final TypeFactory typeFactory = mapper.getTypeFactory();

    private T singleton;
    private ArrayList<T> collection = new ArrayList<>();

    public MogHttpResponse(HttpResponse<String> httpResponse) {
        this.httpResponse = httpResponse;
    }

    public HttpResponse<String> getHttpResponse() {
        return httpResponse;
    }

    public MogHttpResponse(HttpResponse<String> httpResponse, Class<?> accepts)
            throws MogException {
        this(httpResponse, accepts, "$");
    }

    @SuppressWarnings("unchecked")
    public MogHttpResponse(HttpResponse<String> httpResponse, Class<?> accepts, String jsonPath)
            throws MogException {
        try {
            this.httpResponse = httpResponse;
            log.debug("accepts = " + accepts);
            DocumentContext jsonContext = JsonPath.parse(httpResponse.body());
            JsonPath path = JsonPath.compile(jsonPath);
            Object value = jsonContext.read(path);
            if (!path.isDefinite() || value instanceof Collection) {
                collection = mapper.readValue(jsonContext.jsonString(),
                        typeFactory.constructCollectionType(ArrayList.class, accepts));
            } else {
                singleton = (T) jsonContext.read(jsonPath, accepts);
            }
        } catch (Exception e) {
            throw new MogException(e);
        }
    }

    public T singleton() {
        return (T) singleton;
    }

    public Collection<T> list() {
        return collection;
    }

    @Override
    public String toString() {
        return "MogHttpResponse [httpResponse=" + httpResponse + ", body = " + httpResponse.body() + "]";
    }

}
