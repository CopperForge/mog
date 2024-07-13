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

    @Option(names = "--config", description = "specify MOG config file (defaults to ${MOG_HOME}/config.mog)")
    private String configFile = "${MOG_HOME}/config.mog";

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

    @Override
    public String toString() {
        return "MogCommandOptions [helpRequested=" + helpRequested + ", configFile=" + configFile + ", commands="
                + commands + "]";
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
}
