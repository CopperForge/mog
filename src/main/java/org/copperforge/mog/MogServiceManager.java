package org.copperforge.mog;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.copperforge.mog.annotations.MogService;
import org.copperforge.mog.reflection.MogAnnotationFilter;
import org.copperforge.mog.reflection.MogClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MogServiceManager {

    private static MogServiceManager _instance;
    private Logger log = LoggerFactory.getLogger(Mog.class);
    private final Map<String, Object> services = new HashMap<>();

    private MogServiceManager() throws MogException {
        // discover all the services available and put them in the service map
        try {
            MogClassScanner scanner = new MogClassScanner();
            Set<Class<?>> mogServices = scanner.filter(MogAnnotationFilter.filter(MogService.class)).scan();

            MogService serviceAnnotation;
            Object instance;
            log.info("Finding MogServices ...");
            for (Class<?> service : mogServices) {
                serviceAnnotation = service.getAnnotation(MogService.class);
                instance = service.getDeclaredConstructor().newInstance();
                services.put(serviceAnnotation.name(), instance);
            }
            log.info("Found :: " + services.toString());
        } catch (Exception e) {
            log.error("Unable to process MogServiceManager initialization", e);
            throw new MogException(e);
        }

    }

    public static final MogServiceManager instance() throws MogException {
        if (_instance == null)
            _instance = new MogServiceManager();

        return _instance;
    }

    public Object get(String serviceName) throws MogException {
        Object service = services.get(serviceName);
        if (service == null) throw new MogException("Unable to find service by name " + serviceName);
        return services.get(serviceName);
    }

    public Object get(Class<?> serviceType) throws MogException {
        Optional<Object> service = services.values().stream().filter(s -> serviceType.isInstance(s)).findFirst();
        if (service.isEmpty()) throw new MogException("Unable to find service by type " + serviceType.getName());
        return service.get();
    }

}
