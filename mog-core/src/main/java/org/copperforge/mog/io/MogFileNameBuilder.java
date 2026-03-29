package org.copperforge.mog.io;

import org.copperforge.mog.MogException;
import org.copperforge.mog.var.MogVariableService;

public class MogFileNameBuilder {

    public static String build(String filename) throws MogException {
        MogVariableService varService = new MogVariableService();
        filename = varService.envsubst(filename);
        filename = varService.varsubst(filename);
        return filename;
    }

}
