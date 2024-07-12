package org.copperforge.mog.runner;

import java.lang.reflect.Method;

import org.copperforge.mog.MogCommandOptions;
import org.copperforge.mog.MogException;
import org.copperforge.mog.annotations.MogRunner;
import org.copperforge.mog.command.MogAnnotatedCommand;
import org.copperforge.mog.command.MogCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MogRunner(name = "annotatedCommandRunner", description = "Mog Annotated Command Runner", commandClass = MogAnnotatedCommand.class)
public class MogAnnotatedCommandRunner implements MogCommandRunner<MogAnnotatedCommand> {

    private Logger log = LoggerFactory.getLogger(MogAnnotatedCommandRunner.class);
    
    @Override
    public void run(MogAnnotatedCommand command, MogCommandOptions options) throws MogException {
        run(command, options, new String[] { });
    }

    @Override
    public void run(MogCommand command, MogCommandOptions options, String... args) throws MogException {
        try {
            MogAnnotatedCommand cmd = (MogAnnotatedCommand) command;
            log.info("Running annotated command " + command + " with args " + args);

            Class<?> commandClass = Class.forName(cmd.getClassName());
            log.info("commandClass = " + commandClass);
            log.info("runnable? = " + commandClass.isInstance(Runnable.class));

            String subcommand = options.getSubCommands().stream().findFirst().orElse(null);
            log.info("Looking for subcommand: " + subcommand);

            Object commandObject = commandClass.getConstructor().newInstance();
            for (Method method : commandClass.getDeclaredMethods()) {
                org.copperforge.mog.annotations.MogCommand annotation = method
                        .getAnnotation(org.copperforge.mog.annotations.MogCommand.class);
                if (annotation != null && annotation.name().equals(subcommand)) {
                    log.info("Annotated method = " + method + ", " + annotation.name() + ", " + annotation.description());
                    method.invoke(commandObject); // TODO pass args
                }
            }

        } catch (Exception e) {
            throw new MogException(e);
        }
    }

}
