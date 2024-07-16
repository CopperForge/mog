package org.copperforge.mog.command.runner;

import org.copperforge.mog.MogOptions;
import org.copperforge.mog.MogException;
import org.copperforge.mog.command.MogCommand;
import org.copperforge.mog.command.MogCommandResponse;

public interface MogCommandRunner<T extends MogCommand> {

    MogCommandResponse run(T command, MogOptions options) throws MogException;

    MogCommandResponse run(MogCommand cmd, MogOptions options, String... args) throws MogException;

}
