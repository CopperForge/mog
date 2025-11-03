package org.copperforge.mog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class MogOptions {

    private final List<String> rawArgs = new ArrayList<>();

    @Option(names = { "-h", "--help" }, description = "display help")
    private boolean helpRequested = false;

    @Option(names = "--config", description = "specify MOG config file (defaults to ${MOG_HOME}/etc/config.mog)")
    private String configFile = "${MOG_HOME}/etc/config.mog";

    @Option(names = "--working-dir", description = "specify the working dir to execute the command")
    private String workingDirectory = null;

    @Option(names = "--datasources", description = "path to datasources file or directory")
    private String datasourcesPath = null;

    @Parameters(paramLabel = "COMMANDS", description = "mog commands")
    private final List<String> commands = new ArrayList<>();

    public boolean isHelpRequested() {
        return helpRequested;
    }

    public void setHelpRequested(boolean helpRequested) {
        this.helpRequested = helpRequested;
    }

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands.clear();
        this.commands.addAll(commands);
    }

    public String getCommand() {
        return commands.size() > 0 ? commands.get(0) : null;
    }

    public List<String> getSubCommands() {
        return commands.size() > 0 ? commands.subList(1, commands.size()) : null;
    }

    public List<String> rawArgs() {
        return rawArgs;
    }

    public static MogOptions parse(String... args) {
        MogOptions options = new MogOptions();
        options.rawArgs.clear();
        options.rawArgs.addAll(Arrays.asList(args));
        new CommandLine(options).setUnmatchedArgumentsAllowed(true)
                .parseArgs(args);
        return options;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public String getDatasourcesPath() {
        return datasourcesPath;
    }

    public void setDatasourcesPath(String datasourcesPath) {
        this.datasourcesPath = datasourcesPath;
    }

    @Override
    public String toString() {
        return "MogOptions [rawArgs=" + rawArgs + ", helpRequested=" + helpRequested + ", configFile=" + configFile
                + ", workingDirectory=" + workingDirectory + ", datasourcesPath=" + datasourcesPath + ", commands=" + commands + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((rawArgs == null) ? 0 : rawArgs.hashCode());
        result = prime * result + (helpRequested ? 1231 : 1237);
        result = prime * result + ((configFile == null) ? 0 : configFile.hashCode());
        result = prime * result + ((workingDirectory == null) ? 0 : workingDirectory.hashCode());
        result = prime * result + ((commands == null) ? 0 : commands.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MogOptions other = (MogOptions) obj;
        if (rawArgs == null) {
            if (other.rawArgs != null)
                return false;
        } else if (!rawArgs.equals(other.rawArgs))
            return false;
        if (helpRequested != other.helpRequested)
            return false;
        if (configFile == null) {
            if (other.configFile != null)
                return false;
        } else if (!configFile.equals(other.configFile))
            return false;
        if (workingDirectory == null) {
            if (other.workingDirectory != null)
                return false;
        } else if (!workingDirectory.equals(other.workingDirectory))
            return false;
        if (commands == null) {
            if (other.commands != null)
                return false;
        } else if (!commands.equals(other.commands))
            return false;
        return true;
    }
    
}
