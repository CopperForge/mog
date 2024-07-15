package org.copperforge.mog;

import java.util.List;

public class MogSearchPathsConfig {

    private List<String> commands;

    private List<String> reports;

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> command) {
        this.commands = command;
    }

    public List<String> getReports() {
        return reports;
    }

    public void setReports(List<String> reports) {
        this.reports = reports;
    }

    @Override
    public String toString() {
        return "MogSearchPathsConfig [command=" + commands + ", reports=" + reports + "]";
    }
    
    
}
