package org.copperforge.mog;

import org.copperforge.mog.logging.MogLoggingConfig;

public class MogConfig implements MogObject {

    private String name;

    private String description;

    private MogVariable environment[];

    private String mogUser;

    private String mogGroup;

    private MogLoggingConfig logging;

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

    public MogVariable[] getEnvironment() {
        return environment;
    }

    public void setEnvironment(MogVariable[] environment) {
        this.environment = environment;
    }

    public String getMogUser() {
        return mogUser;
    }

    public void setMogUser(String mogUser) {
        this.mogUser = mogUser;
    }

    public String getMogGroup() {
        return mogGroup;
    }

    public void setMogGroup(String mogGroup) {
        this.mogGroup = mogGroup;
    }

    public MogLoggingConfig getLogging() {
        return logging;
    }

    public void setLogging(MogLoggingConfig logging) {
        this.logging = logging;
    }

    @Override
    public String toString() {
        return "MogConfig [name=" + name + ", description=" + description + ", environment=" + environment
                + ", mogUser=" + mogUser + ", mogGroup=" + mogGroup + ", logging=" + logging + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((environment == null) ? 0 : environment.hashCode());
        result = prime * result + ((mogUser == null) ? 0 : mogUser.hashCode());
        result = prime * result + ((mogGroup == null) ? 0 : mogGroup.hashCode());
        result = prime * result + ((logging == null) ? 0 : logging.hashCode());
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
        MogConfig other = (MogConfig) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (environment == null) {
            if (other.environment != null)
                return false;
        } else if (!environment.equals(other.environment))
            return false;
        if (mogUser == null) {
            if (other.mogUser != null)
                return false;
        } else if (!mogUser.equals(other.mogUser))
            return false;
        if (mogGroup == null) {
            if (other.mogGroup != null)
                return false;
        } else if (!mogGroup.equals(other.mogGroup))
            return false;
        if (logging == null) {
            if (other.logging != null)
                return false;
        } else if (!logging.equals(other.logging))
            return false;
        return true;
    }

    
}
