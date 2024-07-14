package org.copperforge.mog.command;

import java.util.List;
import java.util.ArrayList;

public class MogCommandRequest {

    private String command;

    private String subcommand;

    private final List<String> rawArgs = new ArrayList<>();

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getSubcommand() {
        return subcommand;
    }

    public void setSubcommand(String subcommand) {
        this.subcommand = subcommand;
    }

    public List<String> getRawArgs() {
        return rawArgs;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((command == null) ? 0 : command.hashCode());
        result = prime * result + ((subcommand == null) ? 0 : subcommand.hashCode());
        result = prime * result + ((rawArgs == null) ? 0 : rawArgs.hashCode());
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
        MogCommandRequest other = (MogCommandRequest) obj;
        if (command == null) {
            if (other.command != null)
                return false;
        } else if (!command.equals(other.command))
            return false;
        if (subcommand == null) {
            if (other.subcommand != null)
                return false;
        } else if (!subcommand.equals(other.subcommand))
            return false;
        if (rawArgs == null) {
            if (other.rawArgs != null)
                return false;
        } else if (!rawArgs.equals(other.rawArgs))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "MogCommandRequest [command=" + command + ", subcommand=" + subcommand + ", rawArgs=" + rawArgs + "]";
    }

}
