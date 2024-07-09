package org.copperforge.mog.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MogShellCommand.class, name = "shell"),
        @JsonSubTypes.Type(value = MogScriptCommand.class, name = "script"),
        @JsonSubTypes.Type(value = MogBinaryCommand.class, name = "binary"),
})
public class MogCommand {

    private MogCommandType type;

    private String name;

    private String description;

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

    public String getCommand() {
        return null;
    }

    public MogCommandType getType() {
        return type;
    }

    public void setType(MogCommandType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "MogCommand [type=" + type + ", name=" + name + ", description=" + description + "]";
    }

}
