package org.copperforge.mog;

import java.util.concurrent.ConcurrentHashMap;

public class MogServiceManager {

    private static final MogServiceManager instance = new MogServiceManager();

    private final ConcurrentHashMap<Class<?>, Object> services = new ConcurrentHashMap<>();

    public static MogServiceManager instance() {
        return instance;
    }

    private static class ServiceInitUnchecked extends RuntimeException {
        private static final long serialVersionUID = 1L;
        ServiceInitUnchecked(Throwable cause) { super(cause); }
    }

    public <T> T get(Class<T> serviceClass) throws MogException {
        try {
            Object service = services.computeIfAbsent(serviceClass, cls -> {
                try {
                    return cls.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new ServiceInitUnchecked(e);
                }
            });
            return serviceClass.cast(service);
        } catch (ServiceInitUnchecked e) {
            throw new MogException("Unable to instantiate service: " + serviceClass.getName(), e.getCause());
        }
    }

    public <T> void register(Class<T> serviceClass, T service) {
        Object existing = services.putIfAbsent(serviceClass, service);
        if (existing != null && existing != service) {
            throw new IllegalStateException("Service already registered: " + serviceClass.getName());
        }
    }
}
