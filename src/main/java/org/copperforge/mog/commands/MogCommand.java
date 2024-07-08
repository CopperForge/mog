package org.copperforge.mog.commands;

import java.util.Arrays;

public class MogCommand {

    private String name;

    private String description;

    private String command[];

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String[] getCommand() {
        return command;
    }

    public void setCommand(String... command) {
        this.command = command;
    }

    @Override
    public String toString() {
        return "MogCommand [name=" + name + ", description=" + description + ", command=" + Arrays.toString(command) + "]";
    }

}
