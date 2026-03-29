package org.copperforge.mog.io;

import org.copperforge.mog.MogException;
import org.copperforge.mog.runtime.MogContext;
import org.copperforge.mog.var.MogVariableService;

public class MogFileNameBuilder {

    public static String build(String filename) throws MogException {
        return build(filename, null);
    }

    public static String build(String filename, MogContext context) throws MogException {
        MogVariableService varService = new MogVariableService(context);
        filename = varService.envsubst(filename);
        filename = varService.varsubst(filename);
        return filename;
    }

}
