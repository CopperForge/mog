package org.copperforge.mog.runner;

import java.lang.reflect.Method;

import org.copperforge.mog.MogException;
import org.copperforge.mog.command.MogAnnotatedCommand;
import org.copperforge.mog.command.MogCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MogAnnotatedCommandRunner implements MogCommandRunner<MogAnnotatedCommand> {

    private Logger log = LoggerFactory.getLogger(MogAnnotatedCommandRunner.class);
    
    @Override
    public void run(MogAnnotatedCommand command) throws MogException {
        run(command, new String[] { });
    }

    @Override
    public void run(MogCommand command, String... args) throws MogException {
        try {
            MogAnnotatedCommand cmd = (MogAnnotatedCommand) command;
            log.info("Running annotated command " + command + " with args " + args);

            Class<?> commandClass = Class.forName(cmd.getClassName());
            log.info("commandClass = " + commandClass);
            log.info("runnable? = " + commandClass.isInstance(Runnable.class));

            for (Method method : commandClass.getDeclaredMethods()) {
                org.copperforge.mog.annotations.MogCommand annotation = method
                        .getAnnotation(org.copperforge.mog.annotations.MogCommand.class);
                if (annotation != null) {
                    log.info("Annotated method = " + method.getName());
                }
            }

        } catch (Exception e) {
            throw new MogException(e);
        }
    }

}
