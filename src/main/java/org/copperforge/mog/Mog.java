package org.copperforge.mog;

import org.copperforge.mog.annotations.Moglet;
import org.copperforge.mog.command.MogCommand;
import org.copperforge.mog.command.MogCommandService;
import org.copperforge.mog.env.EnvironmentService;
import org.copperforge.mog.env.MogEnvironmentService;
import org.copperforge.mog.reader.MogReader;
import org.copperforge.mog.runner.MogCommandRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mog {

    private static Logger log = LoggerFactory.getLogger(Mog.class);
    private static Mog mog;
    private MogConfig config;

    @Moglet
    EnvironmentService environmentService;

    @Moglet
    MogCommandService commandService;

    public static Mog mog() {
        return mog;
    }

    public MogConfig config() {
        return config;
    }

    public static void main(String[] args) throws MogException {
        mog = new Mog();
        MogOptions options = MogOptions.parse(args);
        log.info("options = " + options);

        mog.initialize(options);
        mog.run(options);
    }

    public void run(MogOptions options) throws MogException {
        // big todo
        if (options.getCommand() != null) {
            log.info("Finding command " + options.getCommand());
            MogCommand cmd = commandService.get(options.getCommand());
            log.info("command = " + cmd);
            MogCommandRunner<?> runner = commandService.runner(cmd.getClass());
            log.info("runner = " + runner);
            runner.run(cmd, options);
        }

    }

    private void initialize(MogOptions options) throws MogException {
        // initalize the service manager
        MogServiceManager manager = MogServiceManager.instance();
        environmentService = (EnvironmentService) manager.get(MogEnvironmentService.class);

        log.info("configFile = " + options.getConfigFile());
        String configFile = environmentService.envsubst(options.getConfigFile());
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

}
