package org.copperforge.mog;

import java.util.Set;

import org.copperforge.mog.reflection.MogClassScanner;
import org.copperforge.mog.reflection.MogPackageFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mog {

    private static Logger log = LoggerFactory.getLogger(Mog.class);

    public static void main(String[] args) throws MogException {
        Mog mog = new Mog();
        mog.initialize();
        mog.run(args);
    }

    public void run(String... args) throws MogException {
        log.info("Running " + args);
        // big todo
    }

    private void initialize() throws MogException {
        MogClassScanner scanner = new MogClassScanner();
        Set<Class<?>> clazzes = scanner.filter(MogPackageFilter.filter("org.copperforge")).scan();
        log.info("clazzes = " + clazzes);
    }
}
