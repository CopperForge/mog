package org.copperforge.mog;

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
        MogServiceManager.instance();
    }
}
