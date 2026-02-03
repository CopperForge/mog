package org.copperforge.mog.web.support;

import org.springframework.http.HttpStatusCode;

public class MogApiClientException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final String responseBody;

    public MogApiClientException(String message, Throwable cause, HttpStatusCode statusCode, String responseBody) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
