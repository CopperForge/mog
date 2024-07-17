package org.copperforge.mog.command.runner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;

import org.copperforge.mog.MogException;
import org.copperforge.mog.MogOptions;
import org.copperforge.mog.annotations.MogRunner;
import org.copperforge.mog.command.MogAnnotatedCommand;
import org.copperforge.mog.command.MogCommand;
import org.copperforge.mog.command.MogCommandResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MogRunner(name = "annotatedCommandRunner", description = "Mog Annotated Command Runner", commandClass = MogAnnotatedCommand.class)
public class MogAnnotatedCommandRunner implements MogCommandRunner<MogAnnotatedCommand> {

    private Logger log = LoggerFactory.getLogger(MogAnnotatedCommandRunner.class);

    @Override
    public MogCommandResponse run(MogAnnotatedCommand command, MogOptions options) throws MogException {
        return run(command, options, new String[] {});
    }

    @Override
    public MogCommandResponse run(MogCommand command, MogOptions options, String... args) throws MogException {
        MogCommandResponse response = new MogCommandResponse();
        try {
            MogAnnotatedCommand cmd = (MogAnnotatedCommand) command;
            log.debug("Running annotated command " + command + " with args " + args);

            Class<?> commandClass = Class.forName(cmd.getClassName());
            log.debug("commandClass = " + commandClass);
            log.debug("runnable? = " + commandClass.isInstance(Runnable.class));

            String subcommand = options.getSubCommands().stream().findFirst().orElse(null);
            log.debug("Looking for subcommand: " + subcommand);

            Object commandObject = commandClass.getConstructor().newInstance();
            for (Method method : commandClass.getDeclaredMethods()) {
                org.copperforge.mog.annotations.MogCommand annotation = method
                        .getAnnotation(org.copperforge.mog.annotations.MogCommand.class);
                if (annotation != null && annotation.name().equals(subcommand)) {
                    log.debug("Annotated method = " + method + ", " + annotation.name() + ", "
                            + annotation.description());

                    Object invokeResp;
                    if (method.getParameterCount() == 1
                            && method.getParameters()[0].getType().equals(MogOptions.class)) {
                        invokeResp = method.invoke(commandObject, options);
                    } else {
                        invokeResp = method.invoke(commandObject);
                    }

                    if (invokeResp instanceof MogCommandResponse) {
                        return (MogCommandResponse) invokeResp;
                    } else {
                        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(byteOut);
                        oos.writeObject(invokeResp);

                        response.setResponse(new ByteArrayInputStream(byteOut.toByteArray()));
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error running command: ", e);
            response.setReturnCode(-1);
            response.setResponse(
                    new ByteArrayInputStream(e.getLocalizedMessage() != null ? e.getLocalizedMessage().getBytes()
                            : new String("Error running command").getBytes()));
        }
        return response;
    }

}
