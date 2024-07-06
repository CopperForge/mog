package org.copperforge.mog.reflection;

public interface MogClassFilter {

    boolean matches(Class<?> clazz);
    
}
