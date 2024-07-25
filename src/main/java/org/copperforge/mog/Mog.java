package org.copperforge.mog;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.copperforge.mog.annotations.Moglet;
import org.copperforge.mog.command.MogCommand;
import org.copperforge.mog.command.MogCommandResponse;
import org.copperforge.mog.command.MogCommandService;
import org.copperforge.mog.command.runner.MogCommandRunner;
import org.copperforge.mog.config.MogConfig;
import org.copperforge.mog.config.Mogf;
import org.copperforge.mog.reader.MogReader;
import org.copperforge.mog.var.MogVariableService;
import org.copperforge.mog.var.VariableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mog {

    private static Logger log = LoggerFactory.getLogger(Mog.class);
    private static Mog mog;
    private MogConfig config;
    private Mogrifier mogrifier;
    private static MogOptions options;
    private Mogf mogf;
    

    @Moglet
    VariableService environmentService;

    @Moglet
    MogCommandService commandService;

    public static Mog mog() {
        return mog;
    }

    public MogConfig config() {
        return this.config;
    }

    public Mogrifier mogrifier() {
        return this.mogrifier;
    }

    public MogOptions options() {
        return options;
    }

    public Mogf mogf() {
        return mogf;
    }

    public static void main(String[] args) {
        try {
            mog = new Mog();
            options = MogOptions.parse(args);
            log.debug("options = " + options);
            mog.initialize(options);

            if (options.isHelpRequested()) {
                mog.listCommands();
                System.exit(0);
            }

            mog.run(options);
        } catch (MogException e) {
            log.error("Exception occurred:" , e);
        }
    }

    private void listCommands() throws MogException {
        for (MogCommand command : commandService.list()) {
            log.info(command.getName() + " : " + command.getDescription());
        }
    }

    @SuppressWarnings("resource")
    public void run(MogOptions options) throws MogException {
        // big todo
        if (options.getCommand() != null) {
            log.debug("Finding command " + options.getCommand());
            MogCommand cmd = commandService.get(options.getCommand());
            log.debug("command = " + cmd);
            MogCommandRunner<?> runner = commandService.runner(cmd.getClass());
            log.debug("runner = " + runner);
            MogCommandResponse response = runner.run(cmd, options);
            System.out.println(new BufferedReader(new InputStreamReader(response.getResponse())).lines().collect(Collectors.joining("\n")));
        }

    }

    private void initialize(MogOptions options) throws MogException {
        
        log.debug("configFile = " + options.getConfigFile());
        String configFile = new MogVariableService().envsubst(options.getConfigFile());
        log.debug("configFile (after) = " + configFile);
        config = new MogReader<MogConfig>(MogConfig.class).read(configFile);
        log.trace("config = " + config);

        // get mogf
        File mogFile = new File(Mog.mog().config().userHome() + "/.mog");
        if (!mogFile.isFile()) {
            mogFile = new File(Mog.mog().config().mogHome() + "/.mog");
            if (!mogFile.isFile()) mogFile = null;
        }
        log.info("Using mogf of " + mogFile);

        if (mogFile != null) mogf = new MogReader<Mogf>(Mogf.class).read(mogFile);
        else mogf = new Mogf();

        // initalize the service manager
        MogServiceManager manager = MogServiceManager.instance();
        environmentService = (VariableService) manager.get(MogVariableService.class);

        // mogrify this guy
        mogrifier = new Mogrifier().config(config);
        mogrifier.mogrify(this);

        for (Object service : manager.services()) {
            mogrifier.mogrify(service);
        }

        // loads the available commands
        log.debug("commands = " + commandService.find());

    }

}
