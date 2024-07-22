package org.copperforge.mog.command;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MogShellCommand extends MogCommand {

    @JsonProperty("script")
    @JsonAlias({ "command", "commands" })
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> commands;

    private String workingDirectory = System.getProperty("user.dir");

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((commands == null) ? 0 : commands.hashCode());
        result = prime * result + ((workingDirectory == null) ? 0 : workingDirectory.hashCode());
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
        MogShellCommand other = (MogShellCommand) obj;
        if (commands == null) {
            if (other.commands != null)
                return false;
        } else if (!commands.equals(other.commands))
            return false;
        if (workingDirectory == null) {
            if (other.workingDirectory != null)
                return false;
        } else if (!workingDirectory.equals(other.workingDirectory))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "MogShellCommand [commands=" + commands + ", workingDirectory=" + workingDirectory + ", getName()="
                + getName() + ", getDescription()=" + getDescription() + ", getType()=" + getType() + "]";
    }

}
