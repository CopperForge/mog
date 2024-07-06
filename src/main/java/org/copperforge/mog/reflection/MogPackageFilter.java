package org.copperforge.mog.reflection;

public class MogPackageFilter implements MogClassFilter {

    private String packageName;

    public static MogClassFilter filter(String packageName) {
        MogPackageFilter filter = new MogPackageFilter();
        filter.packageName = packageName;
        return filter;
    }

    @Override
    public boolean matches(Class<?> clazz) {
        return clazz.getPackageName().equals(packageName);
    }

}
