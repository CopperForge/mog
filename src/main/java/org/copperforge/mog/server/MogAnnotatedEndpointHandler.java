package org.copperforge.mog.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.copperforge.mog.MogException;
import org.copperforge.mog.server.annotations.MogBody;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

public class MogAnnotatedEndpointHandler extends MogEndpointHandler {

    MogAnnotatedEndpoint annotatedEndpoint;

    public MogAnnotatedEndpointHandler(MogEndpoint endpoint) {
        super(endpoint);
        this.annotatedEndpoint = (MogAnnotatedEndpoint) endpoint;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        System.out.println("endpoint = " + annotatedEndpoint);

        String response = "n/a";
        if (annotatedEndpoint.getCallback() != null && annotatedEndpoint.getCallback() != null) {
            Class<?> returnType = annotatedEndpoint.getCallback().getReturnType();

            if (returnType.equals(String.class)) {
                response = handleStringResponse(exchange);
            } else if (returnType.equals(MogResponse.class)) {
                response = handleMogResponse(exchange);
            }
        }

        exchange.getResponseHeaders()
                .put(Headers.CONTENT_TYPE, annotatedEndpoint.getProduces());
        exchange.getResponseSender().send(response);

    }

    private String handleMogResponse(HttpServerExchange exchange) throws MogException {
        try {
            Parameter[] paramters = annotatedEndpoint.getCallback().getParameters();

            if (paramters.length == 0) {
                MogResponse<?> response = (MogResponse<?>) annotatedEndpoint.getCallback()
                        .invoke(annotatedEndpoint.getController());
                return response.json();
            } else {
                List<Object> values = new ArrayList<>();
                for (Parameter p : paramters) {
                    System.out.println("processsing " + p.getName() + " : " + p.getType().getName());
                    if (p.getType().equals(HeaderMap.class)) {
                        values.add(exchange.getRequestHeaders());
                    } else if (p.getType().equals(HttpServerExchange.class)) {
                        values.add(exchange);
                    } else if (p.getAnnotation(MogBody.class) != null) {
                        // get body from input stream
                        @SuppressWarnings("resource")
                        String text = new BufferedReader(
                                new InputStreamReader(exchange.getInputStream(), StandardCharsets.UTF_8))
                                .lines()
                                .collect(Collectors.joining("\n"));
                        System.out.println("text = " + text);

                        ObjectMapper mapper = new ObjectMapper();
                        Object obj = mapper.readValue(text, p.getType());
                        System.out.println("obj = " + obj);

                        // parse object from json
                        values.add(obj); // TODO
                    } else {
                        values.add(null);
                    }
                }
                MogResponse<?> response = (MogResponse<?>) annotatedEndpoint.getCallback()
                        .invoke(annotatedEndpoint.getController(), values.toArray());
                return response.json();
            }
        } catch (Exception e) {
            throw new MogException(e);
        }

    }

    private String handleStringResponse(HttpServerExchange exchange) throws MogException {
        try {
            return (String) annotatedEndpoint.getCallback().invoke(annotatedEndpoint.getController());
        } catch (Exception e) {
            throw new MogException(e);
        }
    }

}
