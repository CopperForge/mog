package org.copperforge.mog.site;

import org.copperforge.mog.MogObject;
import org.copperforge.mog.archiving.MogArchivingConfig;
import org.copperforge.mog.contexts.MogContexts;
import org.copperforge.mog.data.MogDataSources;
import org.copperforge.mog.devops.MogDevops;
import org.copperforge.mog.teams.MogTeams;

public class MogSite implements MogObject {

    private MogDevops devops;

    private MogContexts contexts;

    private MogArchivingConfig archiving;

    private MogDataSources dataSources;

    private MogTeams teams;

    public MogDevops getDevops() {
        return devops;
    }

    public void setDevops(MogDevops devops) {
        this.devops = devops;
    }

    public MogContexts getContexts() {
        return contexts;
    }

    public void setContexts(MogContexts contexts) {
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((devops == null) ? 0 : devops.hashCode());
        result = prime * result + ((contexts == null) ? 0 : contexts.hashCode());
        result = prime * result + ((archiving == null) ? 0 : archiving.hashCode());
        result = prime * result + ((dataSources == null) ? 0 : dataSources.hashCode());
        result = prime * result + ((teams == null) ? 0 : teams.hashCode());
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
        MogSite other = (MogSite) obj;
        if (devops == null) {
            if (other.devops != null)
                return false;
        } else if (!devops.equals(other.devops))
            return false;
        if (contexts == null) {
            if (other.contexts != null)
                return false;
        } else if (!contexts.equals(other.contexts))
            return false;
        if (archiving == null) {
            if (other.archiving != null)
                return false;
        } else if (!archiving.equals(other.archiving))
            return false;
        if (dataSources == null) {
            if (other.dataSources != null)
                return false;
        } else if (!dataSources.equals(other.dataSources))
            return false;
        if (teams == null) {
            if (other.teams != null)
                return false;
        } else if (!teams.equals(other.teams))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "MogSite [devops=" + devops + ", contexts=" + contexts + ", archiving=" + archiving + ", dataSources="
                + dataSources + ", teams=" + teams + "]";
    }

}
