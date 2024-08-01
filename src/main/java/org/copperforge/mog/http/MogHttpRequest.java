package org.copperforge.mog.http;

import java.util.HashMap;
import java.util.Map;

import org.copperforge.mog.MogBean;

public class MogHttpRequest {
    private String url;
    private MogHttpMethod method;
    private final Map<String, String> parameters = new HashMap<>();
    private final Map<String, String> headers = new HashMap<>();
    private MogBean body;
    private Class<?> accepts;

    public MogHttpRequest url(String url) {
        this.url = url;
        return this;
    }

    public MogHttpRequest method(MogHttpMethod method) {
        this.method = method;
        return this;
    }

    public MogHttpRequest parameter(String key, String value) {
        this.parameters.put(key, value);
        return this;
    }

    public MogHttpRequest header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public MogHttpRequest body(MogBean body) {
        this.body = body;
        return this;
    }

    public MogHttpRequest auth(MogHttpAuthorization authorization) {
        return header("Authorization", authorization.header());
    }

    public boolean hasAuthorizationHeader() {
        return headers.containsKey("Authorization");
    }

    public MogHttpRequest accepts(Class<?> acceptsClass) {
        this.accepts = acceptsClass;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public MogHttpMethod getMethod() {
        return method;
    }

    public void setMethod(MogHttpMethod method) {
        this.method = method;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public MogBean getBody() {
        return body;
    }

    public void setBody(MogBean body) {
        this.body = body;
    }

    public Class<?> getAccepts() {
        return accepts;
    }

    @Override
    public String toString() {
        return "MogHttpRequest [url=" + url + ", method=" + method + ", parameters=" + parameters + ", headers="
                + headers + ", body=" + body + ", accepts=" + accepts + "]";
    }

}
