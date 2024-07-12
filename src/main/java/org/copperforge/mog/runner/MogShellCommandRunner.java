package org.copperforge.mog.runner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.copperforge.mog.MogCommandOptions;
import org.copperforge.mog.MogException;
import org.copperforge.mog.annotations.MogRunner;
import org.copperforge.mog.command.MogCommand;
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
    public void run(MogShellCommand command, MogCommandOptions options) throws MogException {
        log.trace("Running " + command);
        run(command, options, new String[] {});
    }

    @Override
    public void run(MogCommand command, MogCommandOptions options, String... args) throws MogException {
        log.trace("Running " + command + "(" + args + ")");
        try {
            if (command.getCommand() == null)
                throw new NullMogCommandException();
            List<String> commands = new ArrayList<>();
            commands.addAll(prefixes);
            commands.addAll(Arrays.asList(command.getCommand()));
            commands.addAll(suffixes);
            builder.command(commands);

            Process process = builder.start();
            log.trace("process = " + process);
            // return process;
        } catch (IOException e) {
            throw new MogException(e);
        }
    }

}
