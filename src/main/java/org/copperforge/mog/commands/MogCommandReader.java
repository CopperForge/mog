package org.copperforge.mog.commands;

import org.copperforge.mog.reader.MogReader;

public class MogCommandReader extends MogReader<MogCommand> {

    public MogCommandReader() {
        super(MogCommand.class);
    }

}
