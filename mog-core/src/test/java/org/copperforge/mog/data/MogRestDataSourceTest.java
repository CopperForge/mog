package org.copperforge.mog.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.copperforge.mog.MogException;
import org.copperforge.mog.data.filter.MogJsonFilter;
import org.copperforge.mog.runtime.MogContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class MogRestDataSourceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetch_usesGetByDefault_andIncludesBearerToken() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> auth = new AtomicReference<>();
        startServer("/items", exchange -> {
            method.set(exchange.getRequestMethod());
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "{\"data\":[{\"message\":\"ok\"}]}");
        });

        MogRestDataSource dataSource = new MogRestDataSource();
        dataSource.setName("restSource");
        dataSource.setType("rest");
        dataSource.setUrl(baseUrl());
        dataSource.setToken("${api_token}");

        MogJsonFilter filter = new MogJsonFilter();
        filter.setType("json");
        filter.setSuburl("/items");
        filter.setJsonPath("$.data[*]");

        List<? extends MogFetchable> rows = dataSource.fetch(filter,
                MogContext.builder().variable("api_token", "secret-token").build());

        assertEquals("GET", method.get());
        assertEquals("Bearer secret-token", auth.get());
        assertEquals("ok", rows.get(0).get("message"));
    }

    @Test
    void fetch_honorsConfiguredHttpMethod() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        startServer("/submit", exchange -> {
            method.set(exchange.getRequestMethod());
            respond(exchange, 200, "{\"data\":[{\"message\":\"posted\"}]}");
        });

        MogRestDataSource dataSource = new MogRestDataSource();
        dataSource.setName("restSource");
        dataSource.setType("rest");
        dataSource.setUrl(baseUrl());

        MogJsonFilter filter = new MogJsonFilter();
        filter.setType("json");
        filter.setMethod("post");
        filter.setSuburl("/submit");
        filter.setJsonPath("$.data[*]");

        List<? extends MogFetchable> rows = dataSource.fetch(filter, MogContext.builder().build());

        assertEquals("POST", method.get());
        assertEquals("posted", rows.get(0).get("message"));
    }

    @Test
    void fetch_failsClearlyOnNon2xxResponses() throws Exception {
        startServer("/fail", exchange -> respond(exchange, 503, "{\"error\":\"down\"}"));

        MogRestDataSource dataSource = new MogRestDataSource();
        dataSource.setName("restSource");
        dataSource.setType("rest");
        dataSource.setUrl(baseUrl());

        MogJsonFilter filter = new MogJsonFilter();
        filter.setType("json");
        filter.setSuburl("/fail");
        filter.setJsonPath("$.data[*]");

        MogException ex = assertThrows(MogException.class,
                () -> dataSource.fetch(filter, MogContext.builder().build()));

        assertTrue(ex.getMessage().contains("HTTP 503"));
    }

    @Test
    void fetch_honorsConfiguredRequestTimeout() throws Exception {
        startServer("/slow", exchange -> {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200, "{\"data\":[{\"message\":\"slow\"}]}");
        });

        MogRestDataSource dataSource = new MogRestDataSource();
        dataSource.setName("restSource");
        dataSource.setType("rest");
        dataSource.setUrl(baseUrl());
        dataSource.setRequestTimeoutMs("${request_timeout_ms}");

        MogJsonFilter filter = new MogJsonFilter();
        filter.setType("json");
        filter.setSuburl("/slow");
        filter.setJsonPath("$.data[*]");

        MogException ex = assertThrows(MogException.class,
                () -> dataSource.fetch(filter, MogContext.builder()
                        .variable("request_timeout_ms", "25")
                        .build()));

        assertTrue(ex.getCause() instanceof java.net.http.HttpTimeoutException);
    }

    private void startServer(String path, ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(path, exchange -> handler.handle(exchange));
        server.start();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        } finally {
            exchange.close();
        }
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
