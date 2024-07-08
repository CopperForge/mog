package org.copperforge.mog.commands;

public class MogWindowsCommandRunner extends MogSimpleCommandRunner {

    @Override
    protected String[] prefixes() {
        return new String[] { "cmd", "/C" };
    }

}
