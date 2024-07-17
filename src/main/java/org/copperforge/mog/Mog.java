package org.copperforge.mog;

import org.copperforge.mog.annotations.Moglet;
import org.copperforge.mog.command.MogCommand;
import org.copperforge.mog.command.MogCommandService;
import org.copperforge.mog.command.runner.MogCommandRunner;
import org.copperforge.mog.config.MogConfig;
import org.copperforge.mog.reader.MogReader;
import org.copperforge.mog.var.MogVariableService;
import org.copperforge.mog.var.VariableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mog {

    private static Logger log = LoggerFactory.getLogger(Mog.class);
    private static Mog mog;
    private MogConfig config;

    @Moglet
    VariableService environmentService;

    @Moglet
    MogCommandService commandService;

    public static Mog mog() {
        return mog;
    }

    public MogConfig config() {
        return config;
    }

    public static void main(String[] args) {
        try {
            mog = new Mog();
            MogOptions options = MogOptions.parse(args);
            log.debug("options = " + options);

            mog.initialize(options);
            mog.run(options);
        } catch (MogException e) {
            log.error("Exception occurred:" , e);
        }
    }

    public void run(MogOptions options) throws MogException {
        // big todo
        if (options.getCommand() != null) {
            log.debug("Finding command " + options.getCommand());
            MogCommand cmd = commandService.get(options.getCommand());
            log.debug("command = " + cmd);
            MogCommandRunner<?> runner = commandService.runner(cmd.getClass());
            log.debug("runner = " + runner);
            runner.run(cmd, options);
        }

    }

    private void initialize(MogOptions options) throws MogException {
        // initalize the service manager
        MogServiceManager manager = MogServiceManager.instance();
        environmentService = (VariableService) manager.get(MogVariableService.class);

        log.debug("configFile = " + options.getConfigFile());
        String configFile = environmentService.envsubst(options.getConfigFile());
        log.debug("configFile (after) = " + configFile);
        config = new MogReader<MogConfig>(MogConfig.class).read(configFile);
        log.trace("config = " + config);

        // mogrify this guy
        Mogrifier mogrifier = new Mogrifier().config(config);
        mogrifier.mogrify(this);

        for (Object service : manager.services()) {
            mogrifier.mogrify(service);
        }

        // loads the available commands
        log.debug("commands = " + commandService.find());

        // TODO mogrify all moggables

    }

}
