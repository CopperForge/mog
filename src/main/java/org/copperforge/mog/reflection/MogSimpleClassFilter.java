package org.copperforge.mog.reflection;

public class MogSimpleClassFilter implements MogClassFilter {

    protected final Class<?> matchClass;

    public MogSimpleClassFilter(Class<?> clazz) {
        this.matchClass = clazz;
    }

    @Override
    public boolean test(Class<?> t) {
        return t.equals(matchClass);
    }

}
