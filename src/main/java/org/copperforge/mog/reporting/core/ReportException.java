package org.copperforge.mog.reporting.core;

public class ReportException extends Exception {

    public ReportException(String message) {
       super(message);
    }
 
    public ReportException(String message, Throwable cause) {
       super(message, cause);
    }
 
    public ReportException(Throwable cause) {
       super(cause);
    }
 
}
