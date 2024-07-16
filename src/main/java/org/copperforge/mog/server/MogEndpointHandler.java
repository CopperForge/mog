package org.copperforge.mog.server;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class MogEndpointHandler implements HttpHandler {

    private final MogEndpoint endpoint;

    public MogEndpointHandler(MogEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (endpoint instanceof MogAnnotatedEndpoint) {
            new MogAnnotatedEndpointHandler(endpoint).handleRequest(exchange);
        } else {
            exchange.getResponseHeaders()
                    .put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("Hello Baeldung");

        }

    }

}
