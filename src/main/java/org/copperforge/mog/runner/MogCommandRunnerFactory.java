package org.copperforge.mog.runner;

import org.copperforge.mog.MogException;
import org.copperforge.mog.command.MogAnnotatedCommand;
import org.copperforge.mog.command.MogCommandType;

public class MogCommandRunnerFactory {

    public static MogCommandRunner<?> create(Class<?> clazz) throws MogException {
        if (clazz.isInstance(MogAnnotatedCommand.class))
            return new MogAnnotatedCommandRunner();

        return new MogSimpleCommandRunner();
    }

    public static MogCommandRunner<?> create(MogCommandType type) throws MogException {
        switch (type) {
            case MogCommandType.ANNOTATED:
                return new MogAnnotatedCommandRunner();
            
            default:
                return new MogSimpleCommandRunner();
            
        }
    }
    
}
