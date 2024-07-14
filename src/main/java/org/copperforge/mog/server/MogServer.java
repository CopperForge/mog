package org.copperforge.mog.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.copperforge.mog.MogException;
import org.copperforge.mog.annotations.MogCommand;
import org.copperforge.mog.reflection.MogAnnotationFilter;
import org.copperforge.mog.reflection.MogClassScanner;
import org.copperforge.mog.server.annotations.MogController;
import org.copperforge.mog.server.annotations.MogRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.util.HttpString;

@MogCommand(name = "server", description = "Mog Server Control")
public class MogServer {

    private Undertow server;
    private final Map<String, MogEndpoint> endpoints = new HashMap<>();
    private Logger log = LoggerFactory.getLogger(MogServer.class);

    public MogServer() throws MogException {
        System.setProperty("org.jboss.logging.provider", "slf4j");
        findAnnotatedEndpoints();
    }

    @MogCommand(name = "start", description = "start MOG server")
    public void start() {
        RoutingHandler routingHandler = new RoutingHandler();
        for (String path : endpoints.keySet()) {
            MogEndpoint endpoint = endpoints.get(path);
            log.info("Adding endpoint '" + path + "' as " + endpoint);
            routingHandler.add(new HttpString(endpoint.getHttpMethod()), endpoint.getPath(), new MogEndpointHandler(endpoint));
        }
        System.out.println("routingHandler = " + routingHandler);

        server = Undertow.builder().addHttpListener(8080, "localhost").setHandler(new BlockingHandler(routingHandler))
                .build();
        server.start();
    }

    protected Map<String, MogEndpoint> findAnnotatedEndpoints() throws MogException {
        try {
            Set<Class<?>> controllerClasses = new MogClassScanner()
                    .filter(MogAnnotationFilter.filter(MogController.class))
                    .scan();
            endpoints.clear();

            for (Class<?> c : controllerClasses) {
                MogController a = c.getAnnotation(MogController.class);
                Object controller = c.getConstructor().newInstance();

                // get the annotated methods
                Stream.of(c.getDeclaredMethods()).filter(m -> m.getAnnotation(MogRequest.class) != null).forEach(m -> {
                    MogRequest a2 = m.getAnnotation(MogRequest.class);
                    MogAnnotatedEndpoint endpoint = new MogAnnotatedEndpoint();
                    endpoint.setPath(a.path() + a2.path());
                    endpoint.setAccepts(a2.accepts());
                    endpoint.setProduces(a2.produces());
                    endpoint.setController(controller);
                    endpoint.setHttpMethod(a2.method());
                    endpoint.setCallback(m);

                    endpoints.put(a.path() + a2.path(), endpoint);
                });

            }

            return endpoints;
        } catch (Exception e) {
            throw new MogException(e);
        }
    }

}
