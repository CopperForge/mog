package org.copperforge.mog;

import java.util.Arrays;

import org.copperforge.mog.annotations.Moglet;
import org.copperforge.mog.commands.MogCommand;
import org.copperforge.mog.commands.MogCommandReader;
import org.copperforge.mog.commands.MogCommandRunner;
import org.copperforge.mog.commands.MogCommandRunnerFactory;
import org.copperforge.mog.commands.MogCommandService;
import org.copperforge.mog.env.EnvironmentService;
import org.copperforge.mog.reader.MogReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class Mog {

    private static Logger log = LoggerFactory.getLogger(Mog.class);
    private final MogCommandRunner runner = MogCommandRunnerFactory.create();
    private static Mog mog;
    private MogConfig config;

    @Moglet
    EnvironmentService environmentService;

    @Moglet
    MogCommandService commandService;

    @Option(names = { "-h", "--help" }, description = "display help")
    private boolean helpRequested = false;

    @Option(names = "--config", description = "specify MOG config file (defaults to ${MOG_HOME}/config.mog)")
    private String configFile = "${MOG_HOME}/config.mog";

    @Parameters(paramLabel = "COMMANDS", description = "mog commands")
    private String[] commands;

    public static Mog mog() {
        return mog;
    }

    public MogConfig config() {
        return config;
    }

    public static void main(String[] args) throws MogException {
        mog = new Mog();
        new CommandLine(mog).parseArgs(args);
        log.info("mog = " + mog);

        mog.initialize();
        mog.run();
    }

    public void run() throws MogException {
        // big todo
        MogCommand cmd = new MogCommandReader().read("src\\test\\resources\\commands\\example.command.mog");
        log.info("cmd = " + cmd);
        runner.run(cmd);
    }

    private void initialize() throws MogException {
        // initalize the service manager
        MogServiceManager.instance();

        // mogrify this guy
        new Mogrifier().mogrify(this);

        // load the config
        configFile = environmentService.envsubst(configFile);
        config = new MogReader<MogConfig>(MogConfig.class).read(configFile);
        log.trace("config = " + config);

        // loads the available commands
        log.info("commands = " + commandService.list());

        // TODO mogrify all moggables

    }

    @Override
    public String toString() {
        return "Mog [helpRequested=" + helpRequested + ", configFile=" + configFile
                + ", commands = " + (commands != null ? Arrays.asList(commands) : null) + "]";
    }
}
