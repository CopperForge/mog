package org.copperforge.mog.reflection;

import java.util.HashSet;
import java.util.Set;

import org.copperforge.mog.MogException;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

public class MogClassScanner {

    public MogClassScanner() {
    }

    public Set<Class<?>> scan(MogClassFilter filter) throws MogException {
        try {
            Set<Class<?>> matches = new HashSet<>();

            ImmutableSet<ClassInfo> clazzes = ClassPath.from(ClassLoader.getSystemClassLoader()).getAllClasses();
            clazzes.stream().forEach(info -> {
                try {
                    Class<?> clazz = info.load();

                    if (filter.test(clazz)) {
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
