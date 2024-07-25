package org.copperforge.mog.reflection;

import java.util.function.Predicate;

public class MogPackageFilter implements Predicate<String> {

    private String matchPackage;

    public MogPackageFilter(String matchPackage) {
        this.matchPackage = matchPackage;
    }

    @Override
    public boolean test(String t) {
        if (t == null && matchPackage == null) return true;
        if (t != null) return t.equals(matchPackage);
        else return false;
    }

}
