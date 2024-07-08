package org.copperforge.mog.commands;

import java.io.FileInputStream;
import java.util.Arrays;

import org.copperforge.mog.MogException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    public static MogCommand load(final String filename) throws MogException {
        try {
            FileInputStream is = new FileInputStream(filename);
            ObjectMapper mapper = new ObjectMapper(); // todo make a singleton
            MogCommand cmd = mapper.readValue(is, MogCommand.class);
            return cmd;
        } catch (Exception e) {
            throw new MogException("Unable to load config file :: " + filename, e);
        }
    }

    @Override
    public String toString() {
        return "MogCommand [name=" + name + ", description=" + description + ", command=" + Arrays.toString(command)
                + ", executable=" + executable + "]";
    }

}
