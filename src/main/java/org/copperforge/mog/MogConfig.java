package org.copperforge.mog;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.copperforge.mog.archiving.MogArchivingConfig;
import org.copperforge.mog.contexts.MogContext;
import org.copperforge.mog.data.MogDataSources;
import org.copperforge.mog.devops.MogDevops;
import org.copperforge.mog.logging.MogLoggingConfig;
import org.copperforge.mog.teams.MogTeams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MogConfig implements MogObject {

    private static MogConfig _config;

    private MogVariable environment[];

    private String mogUser;

    private String mogGroup;

    private MogLoggingConfig logging;

    private String name;

    private String description;

    private MogDevops devops;

    private List<MogContext> contexts;

    private MogArchivingConfig archiving;

    private MogDataSources dataSources;

    private MogTeams teams;

    private MogConfig() {

    }

    public MogDevops getDevops() {
        return devops;
    }

    public void setDevops(MogDevops devops) {
        this.devops = devops;
    }

    public List<MogContext> getContexts() {
        return contexts;
    }

    public void setContexts(List<MogContext> contexts) {
        this.contexts = contexts;
    }

    public MogArchivingConfig getArchiving() {
        return archiving;
    }

    public void setArchiving(MogArchivingConfig archiving) {
        this.archiving = archiving;
    }

    public MogDataSources getDataSources() {
        return dataSources;
    }

    public void setDataSources(MogDataSources dataSources) {
        this.dataSources = dataSources;
    }

    public MogTeams getTeams() {
        return teams;
    }

    public void setTeams(MogTeams teams) {
        this.teams = teams;
    }

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

    public static MogConfig load(final String filename) throws MogException {
        try {
            FileInputStream is = new FileInputStream(filename);
            ObjectMapper mapper = new ObjectMapper(); // todo make a singleton
            _config = mapper.readValue(is, MogConfig.class);
            return _config;
        } catch (Exception e) {
            throw new MogException("Unable to load config file :: " + filename, e);
        }
    }

    public static MogConfig config() {
        return _config;
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
