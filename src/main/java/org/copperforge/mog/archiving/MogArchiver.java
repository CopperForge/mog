package org.copperforge.mog.archiving;

import org.copperforge.mog.annotations.MogCommand;
import org.copperforge.mog.command.MogCommandResponse;

@MogCommand(name = "archive", description = "Archiving tool using defined archive sets")
public class MogArchiver {

    @MogCommand(name = "deflate", description = "Deflate the given archive set")
    public MogCommandResponse deflate() {
        MogCommandResponse response = new MogCommandResponse();

        return response;
    }

    @MogCommand(name = "inflate", description = "Inflate the given archive set")
    public MogCommandResponse inflate() {
        MogCommandResponse response = new MogCommandResponse();

        return response;
    }

}
