package org.copperforge.mog;

import java.io.File;

import org.copperforge.mog.config.MogConfig;
import org.copperforge.mog.config.Mogf;
import org.copperforge.mog.reader.MogReader;
import org.copperforge.mog.var.MogVariableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mog {

    private static Logger log = LoggerFactory.getLogger(Mog.class);
    private static Mog mog;
    private MogConfig config;
    private static MogOptions options;
    private Mogf mogf;

    public static Mog mog() {
        return mog;
    }

    public MogConfig config() {
        return this.config;
    }

    public MogOptions options() {
        return options;
    }

    public Mogf mogf() {
        return mogf;
    }

    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    public static void main(String[] args) {
        try {
            mog = new Mog();
            options = MogOptions.parse(args);
            log.debug("options = " + options);
            mog.initialize(options);

            // todo run command

        } catch (MogException e) {
            log.info("ERROR: " + e.getLocalizedMessage());
            // log.error("Exception occurred:", e);
        }
    }

    private void initialize(MogOptions options) throws MogException {

        log.info("mog v" + getVersion());
        log.info("");

        log.debug("configFile = " + options.getConfigFile());
        String configFile = new MogVariableService().envsubst(options.getConfigFile());
        log.debug("configFile (after) = " + configFile);
        config = new MogReader<MogConfig>(MogConfig.class).read(configFile);
        log.trace("config = " + config);

        // get mogf
        File mogFile = new File(Mog.mog().config().userHome() + "/.mog");
        if (!mogFile.isFile()) {
            mogFile = new File(Mog.mog().config().mogHome() + "/.mog");
            if (!mogFile.isFile())
                mogFile = null;
        }
        log.debug("Using mogf of " + mogFile);

        if (mogFile != null)
            mogf = new MogReader<Mogf>(Mogf.class).read(mogFile);
        else
            mogf = new Mogf();

    }

}
