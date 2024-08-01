package org.copperforge.mog.http;

public class MogTokenAuthorization implements MogHttpAuthorization {

    private final String token;

    protected MogTokenAuthorization(String token) {
        this.token = token;
    }

    @Override
    public String header() {
        return "Bearer " + token;
    }

    public static MogHttpAuthorization create(String token) {
        return new MogTokenAuthorization(token);        
    }

}
