package org.copperforge.mog;

public class MogException extends Exception {

    public MogException() {
    }

    public MogException(String msg) {
        super(msg);
    }

    public MogException(Throwable e) {
        super(e);
    }

    public MogException(Exception e) {
        super(e);
    }

    public MogException(String msg, Throwable e) {
        super(msg, e);
    }
    
}
