package org.copperforge.mog;

import java.util.Arrays;

import org.copperforge.mog.annotations.Moglet;
import org.copperforge.mog.command.MogCommand;
import org.copperforge.mog.command.MogCommandService;
import org.copperforge.mog.env.EnvironmentService;
import org.copperforge.mog.env.MogEnvironmentService;
import org.copperforge.mog.reader.MogReader;
import org.copperforge.mog.runner.MogCommandRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class Mog {

    private static Logger log = LoggerFactory.getLogger(Mog.class);
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
        mog.run(args);
    }

    public void run(String... args) throws MogException {
        // big todo
        String command = null;
        if (commands != null && commands.length > 0) {
            command = commands[0];
            log.info("Finding command " + command);
            MogCommand cmd = commandService.get(command);
            log.info("command = " + cmd);
            MogCommandRunner<?> runner = commandService.runner(cmd.getClass());
            log.info("runner = " + runner);
            runner.run(cmd, args);
        }

    }

    private void initialize() throws MogException {
        // initalize the service manager
        MogServiceManager manager = MogServiceManager.instance();
        environmentService = (EnvironmentService) manager.get(MogEnvironmentService.class);

        log.info("configFile = " + configFile);
        configFile = environmentService.envsubst(configFile);
        log.info("configFile (after) = " + configFile);
        config = new MogReader<MogConfig>(MogConfig.class).read(configFile);
        log.trace("config = " + config);

        // mogrify this guy
        Mogrifier mogrifier = new Mogrifier().config(config);
        mogrifier.mogrify(this);

        for (Object service : manager.services()) {
            mogrifier.mogrify(service);
        }

        // loads the available commands
        log.info("commands = " + commandService.find());

        // TODO mogrify all moggables

    }

    @Override
    public String toString() {
        return "Mog [helpRequested=" + helpRequested + ", configFile=" + configFile
                + ", commands = " + (commands != null ? Arrays.asList(commands) : null) + "]";
    }

}
