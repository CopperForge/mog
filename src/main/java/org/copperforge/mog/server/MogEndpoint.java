package org.copperforge.mog.server;

public interface MogEndpoint {

    String getPath();

    String getAccepts();

    String getProduces();

    String getHttpMethod();

}
