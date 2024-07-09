package org.copperforge.mog.command;

import org.copperforge.mog.MogException;

public class NullMogCommandException extends MogException {

    public NullMogCommandException() {
        super("Mog command cannot be null");
    }

}
