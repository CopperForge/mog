package org.copperforge.mog.io;

import org.copperforge.mog.MogException;
import org.copperforge.mog.MogServiceManager;
import org.copperforge.mog.MogVariableService;
import org.copperforge.mog.VariableService;

public class FileNameBuilder {

    public static String build(String filename) throws MogException {
        VariableService varService = (VariableService) MogServiceManager.instance().get(MogVariableService.class);
        filename = varService.envsubst(filename);
        filename = varService.varsubst(filename);
        return filename;
    }

}
