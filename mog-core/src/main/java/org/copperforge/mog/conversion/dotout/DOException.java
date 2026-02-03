package org.copperforge.mog.conversion.dotout;

public class DOException extends Exception {

    public DOException(String message) {
       super(message);
    }
 
    public DOException(String message, Throwable cause) {
       super(message, cause);
    }
 
    public DOException(Throwable cause) {
       super(cause);
    }
 
}
