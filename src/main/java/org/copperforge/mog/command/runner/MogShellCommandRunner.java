package org.copperforge.mog.command.runner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.copperforge.mog.MogException;
import org.copperforge.mog.MogOptions;
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
        MogShellCommand shellCommand = (MogShellCommand) command;

        MogCommandResponse response = new MogCommandResponse();
        try {
            log.debug("shell command = " + command);

            if (shellCommand.getCommands() == null || shellCommand.getCommands().isEmpty())
                throw new NullMogCommandException();

            List<String> commands = new ArrayList<>();
            commands.addAll(prefixes());
            commands.addAll(shellCommand.getCommands());
            commands.addAll(suffixes());
            builder.command(commands);

            if (options.getWorkingDirectory() != null) {
                builder.directory(new File(options.getWorkingDirectory()));
            } else {
                builder.directory(new File(shellCommand.getWorkingDirectory()));
            }

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

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    protected List<String> prefixes() {
        List<String> prefixes = new ArrayList<>();
        if (isWindows()) {
            prefixes.add("cmd.exe");
            prefixes.add("/c");
        } else { // for now, assume unix
            prefixes.add("/bin/sh");
            prefixes.add("-c");
        }
        return prefixes;
    }

    protected List<String> suffixes() {
        return new ArrayList<>();
    }

}
