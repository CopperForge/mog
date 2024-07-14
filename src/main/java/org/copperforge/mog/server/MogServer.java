package org.copperforge.mog.server;

import org.copperforge.mog.annotations.MogCommand;

import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.server.HttpHandler;
import io.undertow.util.Headers;

@MogCommand(name = "server", description = "Mog Server Control")
public class MogServer {

    private Undertow server;

    @MogCommand(name = "start", description = "start MOG server")
    public void start() {
        Builder builder = Undertow.builder().addHttpListener(8080, "localhost");

        // HttpHandler handler = new HttpHandler();

        // TODO gather endpoints
        builder.setHandler(exchange -> {
            exchange.getResponseHeaders()
                    .put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("Hello Baeldung");
        });

        server = builder.build();
        server.start();
    }

}
