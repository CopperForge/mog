package org.copperforge.mog.reflection;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MogAnnotationFilter extends MogSimpleClassFilter {

    public MogAnnotationFilter(Class<?> clazz) {
        super(clazz);
    }

    @Override
    public boolean test(Class<?> clazz) {
        List<Class<?>> annotations = Arrays.asList(clazz.getAnnotations()).stream().map(a -> a.annotationType()).collect(Collectors.toList());
        return annotations.contains(matchClass);
    }

}
