package org.copperforge.mog.command.runner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.copperforge.mog.MogOptions;
import org.copperforge.mog.MogException;
import org.copperforge.mog.annotations.MogRunner;
import org.copperforge.mog.command.MogCommand;
import org.copperforge.mog.command.MogCommandResponse;
import org.copperforge.mog.command.MogShellCommand;
import org.copperforge.mog.command.NullMogCommandException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MogRunner(name = "shellRunner", description = "Mog Shell Command Runner", commandClass = MogShellCommand.class)
public class MogShellCommandRunner implements MogCommandRunner<MogShellCommand> {

    private final ProcessBuilder builder;
    private Logger log = LoggerFactory.getLogger(MogShellCommandRunner.class);
    private final List<String> prefixes = new ArrayList<>();
    private final List<String> suffixes = new ArrayList<>();

    public MogShellCommandRunner() {
        builder = new ProcessBuilder().redirectErrorStream(true);
    }

    @Override
    public MogCommandResponse run(MogShellCommand command, MogOptions options) throws MogException {
        log.trace("Running " + command);
        return run(command, options, new String[] {});
    }

    @Override
    public MogCommandResponse run(MogCommand command, MogOptions options, String... args) throws MogException {
        log.trace("Running " + command + "(" + args + ")");
        MogCommandResponse response = new MogCommandResponse();
        try {
            if (command.getCommand() == null)
                throw new NullMogCommandException();
            List<String> commands = new ArrayList<>();
            commands.addAll(prefixes);
            commands.addAll(Arrays.asList(command.getCommand()));
            commands.addAll(suffixes);
            builder.command(commands);

            Process process = builder.start();
            response.setResponse(process.getInputStream());
            log.trace("process = " + process);
        } catch (IOException e) {
            log.error("Error running command: ", e);
            response.setReturnCode(-1);
            response.setResponse(new ByteArrayInputStream(e.getMessage().getBytes()));
        }
        return response;
    }

}
