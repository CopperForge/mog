package org.copperforge.mog;

import java.util.concurrent.ConcurrentHashMap;

public class MogServiceManager {

    private static MogServiceManager instance = new MogServiceManager();

    private final ConcurrentHashMap<Class<?>, Object> services = new ConcurrentHashMap<>();

    public static MogServiceManager instance() {
        return instance;
    }

    public Object get(Class<?> serviceClass) throws MogException {
        Object service = services.get(serviceClass);
        if (service == null) {
            try {
                service = serviceClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new MogException("Unable to instantiate service: " + serviceClass.getName(), e);
            }
        }
        return service;
    }

    public void register(Class<?> serviceClass, Object service) {
        services.put(serviceClass, service);
    }
}
