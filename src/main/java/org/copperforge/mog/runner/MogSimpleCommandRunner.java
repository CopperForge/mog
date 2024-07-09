package org.copperforge.mog.runner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.copperforge.mog.MogException;
import org.copperforge.mog.command.MogAnnotatedCommand;
import org.copperforge.mog.command.MogCommand;
import org.copperforge.mog.command.MogCommandType;
import org.copperforge.mog.command.NullMogCommandException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MogSimpleCommandRunner implements MogCommandRunner<MogCommand> {

    private Logger log = LoggerFactory.getLogger(MogSimpleCommandRunner.class);

    private final ProcessBuilder builder;

    public MogSimpleCommandRunner() {
        builder = new ProcessBuilder().redirectErrorStream(true);
    }

    protected String[] prefixes() {
        return new String[] {};
    }

    protected String[] suffixes() {
        return new String[] {};
    }

    @Override
    public void run(MogCommand command) throws MogException {
        try {
            if (command.getCommand() == null)
                throw new NullMogCommandException();
            List<String> commands = new ArrayList<>();
            commands.addAll(Arrays.asList(prefixes()));
            commands.addAll(Arrays.asList(command.getCommand()));
            commands.addAll(Arrays.asList(suffixes()));
            builder.command(commands);
            Process process = builder.start();
            //return process;
        } catch (IOException e) {
            throw new MogException(e);
        }
    }

    @Override
    public void run(MogCommand command, String... args) throws MogException {
        switch (command.getType()) {
            case MogCommandType.ANNOTATED:
                runAnnotatedCommand((MogAnnotatedCommand) command, args);
            default:
                throw new MogException("Type " + command.getType() + " not yet implemented");
        }
    }

    protected Process runAnnotatedCommand(MogAnnotatedCommand command, String... args) throws MogException {


        return null;
    }

}
