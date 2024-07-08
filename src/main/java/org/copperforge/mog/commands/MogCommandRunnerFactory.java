package org.copperforge.mog.commands;

public class MogCommandRunnerFactory {

    public static MogCommandRunner create() {
        MogCommandRunner runner;
        if (System.getProperty("os.name").startsWith("Windows")) {
            runner = new MogWindowsCommandRunner();
        } else {
            runner = new MogLinuxCommandRunner();
        }
        return runner;
    }
    
}
