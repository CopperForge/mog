package org.copperforge.mog;

import org.copperforge.mog.config.MogConfig;
import org.copperforge.mog.config.Mogf;
import org.copperforge.mog.reporting.ReportingCommand;
import org.copperforge.mog.security.MogDecryptCommand;
import org.copperforge.mog.security.MogEncryptCommand;
import org.copperforge.mog.runtime.MogContext;
import org.copperforge.mog.runtime.MogRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Mixin;
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
    private MogContext context;

    // Accept root-level options like --config via Picocli without rejecting them
    @Mixin
    private MogOptions cliOptions;

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

    public MogContext context() {
        return context;
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
            mog.context = buildContext(options, mog.config(), mog.mogf());

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

        config = MogRuntime.loadConfig(options.getConfigFile());
        log.trace("config = " + config);

        mogf = MogRuntime.loadMogf(config);
        log.debug("Loaded mogf settings");

    }

    private static MogContext buildContext(MogOptions options, MogConfig config, Mogf mogf) {
        String mogHome = config != null ? config.mogHome() : System.getenv("MOG_HOME");
        String mogEtc = System.getenv("MOG_ETC");
        return MogContext.builder()
                .config(config)
                .mogf(mogf)
                .datasourcesPath(options.getDatasourcesPath())
                .environment(options.getEnv())
                .workingDirectory(options.getWorkingDirectory())
                .mogHome(mogHome)
                .mogEtc(mogEtc)
                .build();
    }
}
