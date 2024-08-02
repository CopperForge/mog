package org.copperforge.mog.scm;

import java.io.Serializable;

import org.copperforge.mog.git.MogGitConfig;
import org.copperforge.mog.gitea.MogGitea;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MogGitConfig.class, name = "git"),
        @JsonSubTypes.Type(value = MogGitea.class, name = "gitea")
})
public class MogScm implements Serializable {

    private String type;

    private String name;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
