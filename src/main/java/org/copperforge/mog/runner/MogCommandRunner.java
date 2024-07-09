package org.copperforge.mog.runner;

import org.copperforge.mog.MogException;
import org.copperforge.mog.command.MogCommand;

public interface MogCommandRunner<T extends MogCommand> {

    void run(T command) throws MogException;

    void run(MogCommand cmd, String... args) throws MogException;

}
