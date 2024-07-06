package org.copperforge.mog.reflection;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MogAnnotationFilter implements MogClassFilter {

    private Class<?> annotationClazz;

    public static MogClassFilter filter(Class<?> clazz) {
        MogAnnotationFilter filter = new MogAnnotationFilter();
        filter.annotationClazz = clazz;
        return filter;
    }

    @Override
    public boolean matches(Class<?> clazz) {
        List<Class<?>> annotations = Arrays.asList(clazz.getAnnotations()).stream().map(a -> a.annotationType()).collect(Collectors.toList());
        return annotations.contains(annotationClazz);
    }

}
