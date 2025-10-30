package org.copperforge.mog;

import java.io.File;

import org.copperforge.mog.config.MogConfig;
import org.copperforge.mog.config.Mogf;
import org.copperforge.mog.reader.MogReader;
import org.copperforge.mog.reporting.ReportingCommand;
import org.copperforge.mog.security.MogDecryptCommand;
import org.copperforge.mog.security.MogEncryptCommand;
import org.copperforge.mog.var.MogVariableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "mog",
    description = "MOG - It is what you make it",
    mixinStandardHelpOptions = true,
    subcommands = {
        ReportingCommand.class,
        MogEncryptCommand.class,
        MogDecryptCommand.class
    }
)
public class Mog implements Runnable {

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

            // hand off to picocli subcommands
            int exitCode = new CommandLine(new Mog()).execute(args);
            if (exitCode != 0) {
                System.exit(exitCode);
            }

        } catch (MogException e) {
            log.info("ERROR: " + e.getLocalizedMessage());
            // log.error("Exception occurred:", e);
        }
    }

    @Override
    public void run() {
        // Root command; if no subcommand provided, show usage header
        new CommandLine(this).usage(System.out);
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
