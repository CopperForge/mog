package org.copperforge.mog.reflection;

import java.util.HashSet;
import java.util.Set;

import org.copperforge.mog.MogException;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

public class MogClassScanner {

    private final Set<MogClassFilter> filters = new HashSet<>();

    public MogClassScanner filter(MogClassFilter filter) {
        filters.add(filter);
        return this;
    }

    public Set<Class<?>> scan() throws MogException {
        try {
            Set<Class<?>> matches = new HashSet<>();

            ImmutableSet<ClassInfo> clazzes = ClassPath.from(ClassLoader.getSystemClassLoader()).getAllClasses();
            clazzes.stream().forEach(info -> {
                try {
                    Class<?> clazz = info.load();

                    if (filters.stream().anyMatch(f -> f.matches(clazz))) {
                        matches.add(clazz);
                    }
                } catch (NoClassDefFoundError cnfe) {
                    // eat it, but log it
                }
            });

            return matches;
        } catch (Exception e) {
            throw new MogException(e);
        }
    }

}
