package org.copperforge.mog.commands;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MogCommand {

    private String name;

    private String description;

    private String command[];

    @JsonProperty("script")
    private String executable;

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

    public String getExecutable() {
        return executable;
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    @Override
    public String toString() {
        return "MogCommand [name=" + name + ", description=" + description + ", command=" + Arrays.toString(command)
                + ", executable=" + executable + "]";
    }

}
