package org.copperforge.mog.server;

import java.lang.reflect.Method;

public class MogAnnotatedEndpoint implements MogEndpoint {

    private String path;

    private String accepts;

    private String produces;

    private Object controller;

    private Method callback;

    private String method;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getAccepts() {
        return accepts;
    }

    public void setAccepts(String accepts) {
        this.accepts = accepts;
    }

    public String getProduces() {
        return produces;
    }

    public void setProduces(String produces) {
        this.produces = produces;
    }

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }

    public Method getCallback() {
        return callback;
    }

    public void setCallback(Method callback) {
        this.callback = callback;
    }

    public String getHttpMethod() {
        return method;
    }

    public void setHttpMethod(String method) {
        this.method = method;
    }

    
    
}
