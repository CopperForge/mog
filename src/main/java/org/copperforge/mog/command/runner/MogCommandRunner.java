package org.copperforge.mog.runner;

import org.copperforge.mog.MogOptions;
import org.copperforge.mog.MogException;
import org.copperforge.mog.command.MogCommand;

public interface MogCommandRunner<T extends MogCommand> {

    void run(T command, MogOptions options) throws MogException;

    void run(MogCommand cmd, MogOptions options, String... args) throws MogException;

}
