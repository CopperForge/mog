package org.copperforge.mog.server;

import java.io.Serializable;

import org.copperforge.mog.MogException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MogResponse<T extends Serializable> {

    private T response;
    private int status;

    public MogResponse(T response, int status) {
        this.response = response;
        this.status = status;
    }

    public MogResponse<T> response(T response) {
        this.response = response;
        return this;
    }

    public MogResponse<T> status(int status) {
        this.status = status;
        return this;
    }

    public T response() {
        return response;
    }

    public int status() {
        return status;
    }

    public String json() throws MogException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(mapper);
        } catch (Exception e) {
            throw new MogException(e);
        }
    }

}
