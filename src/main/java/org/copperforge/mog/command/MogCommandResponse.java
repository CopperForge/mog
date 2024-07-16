package org.copperforge.mog.command;

import java.io.InputStream;
import java.io.Serializable;

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
        
}
