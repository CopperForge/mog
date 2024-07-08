package org.copperforge.mog.commands;

import org.copperforge.mog.MogException;

public interface MogCommandRunner {

    void run(MogCommand command) throws MogException;
    
}
