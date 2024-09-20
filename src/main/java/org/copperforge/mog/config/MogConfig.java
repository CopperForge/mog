package org.copperforge.mog.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.copperforge.mog.MogObject;
import org.copperforge.mog.archiving.MogArchiveSet;
import org.copperforge.mog.data.MogDataSource;
import org.copperforge.mog.devops.MogDevops;
import org.copperforge.mog.var.MogVariable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MogConfig implements MogObject {

    private MogVariable environment[];

    private String mogUser;

    private String mogGroup;

    private String name;

    private String description;

    private List<MogDataSource> dataSources = new ArrayList<>();

    private MogSearchPathsConfig searchPaths;

    private List<MogArchiveSet> archiveSets;

    private MogDevops devops;

    private MogConfig() {

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

    public List<MogDataSource> getDataSources() {
        return dataSources;
    }

    public void setDataSources(List<MogDataSource> dataSources) {
        this.dataSources = dataSources;
    }

    public MogSearchPathsConfig getSearchPaths() {
        return searchPaths;
    }

    public void setSearchPaths(MogSearchPathsConfig searchPaths) {
        this.searchPaths = searchPaths;
    }

    public List<MogArchiveSet> getArchiveSets() {
        return archiveSets;
    }

    public void setArchiveSets(List<MogArchiveSet> archiveSets) {
        this.archiveSets = archiveSets;
    }

    public String userHome() {
        return System.getProperty("user.home");
    }

    public String mogHome() {
        return System.getenv("MOG_HOME");
    }

    public MogDevops getDevops() {
        return devops;
    }

    public void setDevops(MogDevops devops) {
        this.devops = devops;
    }

    @Override
    public String toString() {
        return "MogConfig [environment=" + Arrays.toString(environment)
                + ", mogUser=" + mogUser + ", mogGroup=" + mogGroup + ", name=" + name + ", description=" + description
                + ", dataSources=" + dataSources + ", searchPaths=" + searchPaths + ", archiveSets=" + archiveSets
                + ", devops=" + devops + "]";
    }

}
