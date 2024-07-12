package org.copperforge.mog.runner;

import org.copperforge.mog.MogCommandOptions;
import org.copperforge.mog.MogException;
import org.copperforge.mog.command.MogCommand;

public interface MogCommandRunner<T extends MogCommand> {

    void run(T command, MogCommandOptions options) throws MogException;

    void run(MogCommand cmd, MogCommandOptions options, String... args) throws MogException;

}
