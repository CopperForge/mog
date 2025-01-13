package org.copperforge.mog.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

public class MogCommandResponse implements Serializable {

    private int returnCode = 0;

    private InputStream response;

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public InputStream getResponse() {
        return response;
    }

    public void setResponse(InputStream response) {
        this.response = response;
    }
 
    @Override
    public String toString() {
        String resp = "null";
        try {
            System.out.println("response = " + response);
            if (response != null)
                resp = IOUtils.toString(response, StandardCharsets.UTF_8);
        } catch (IOException e) {
            resp = String.format("Error: %s", e.getLocalizedMessage());
        }
        System.out.println("resp = '" + resp + "'");
        return resp;
    }
}
