package org.copperforge.mog;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;

import org.copperforge.mog.annotations.Moglet;
import org.copperforge.mog.commands.MogCommand;
import org.copperforge.mog.commands.MogCommandRunner;
import org.copperforge.mog.commands.MogCommandRunnerFactory;
import org.copperforge.mog.env.EnvironmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class Mog {

    private static Logger log = LoggerFactory.getLogger(Mog.class);

    private MogServiceManager serviceManager;
    private final MogCommandRunner runner = MogCommandRunnerFactory.create();

    private MogConfig config;

    @Moglet
    EnvironmentService environmentService;

    @Option(names = { "-h", "--help" }, description = "display help")
    private boolean helpRequested = false;

    @Option(names = "--config", description = "specify MOG config file (defaults to ${MOG_HOME}/config.mog)")
    private String configFile = "${MOG_HOME}/config.mog";

    @Option(names = "--site", description = "specify site configuration file")
    private String siteFile = "${MOG_HOME}/site.mog";

    @Parameters(paramLabel = "COMMANDS", description = "mog commands")
    private String[] commands;

    public static void main(String[] args) throws MogException {
        Mog mog = new Mog();
        new CommandLine(mog).parseArgs(args);
        log.info("mog = " + mog);

        mog.initialize();
        mog.run();
    }

    public void run() throws MogException {
        // big todo
        MogCommand cmd = MogCommand.load("src\\test\\resources\\commands\\example.command.mog");
        log.info("cmd = " + cmd);
        runner.run(cmd);
    }

    private void initialize() throws MogException {
        serviceManager = MogServiceManager.instance();
        transmogrify(this);
        configFile = environmentService.envsubst(configFile);
        config = MogConfig.load(configFile);
        log.info("config = " + config);

        siteFile = environmentService.envsubst(siteFile);
    }

    private void transmogrify(Object moggable) throws MogException {
        log.trace("Transmogrifying :: " + moggable.getClass());
        try {

            Field[] fields = moggable.getClass().getDeclaredFields();
            for (Field field : fields) {
                Annotation[] fieldAnnotations = field.getAnnotationsByType(Moglet.class);
                for (Annotation annotation : fieldAnnotations) {
                    if (annotation.annotationType().equals(Moglet.class)) {
                        Moglet moglet = (Moglet) annotation;
                        if (moglet.name().isEmpty()) {
                            Class<?> mogletClass = moglet.type();
                            if (mogletClass.equals(Object.class)) {
                                mogletClass = field.getType();
                            }
                            field.set(this, serviceManager.get(mogletClass));
                        } else {
                            field.set(this, serviceManager.get(moglet.name()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Exception during transomogrification of " + moggable.getClass().getName(), e);
            throw new MogException("Error during transmogrification", e);
        }
    }

    @Override
    public String toString() {
        return "Mog [helpRequested=" + helpRequested + ", configFile=" + configFile + ", siteFile=" + siteFile
                + ", commands = " + (commands != null ? Arrays.asList(commands) : null) + "]";
    }
}
