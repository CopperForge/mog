package org.copperforge.mog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.copperforge.mog.data.MogDataSource;
import org.copperforge.mog.server.MogServerConfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MogConfig implements MogObject {

    private MogServerConfig serverConfig;

    private MogVariable environment[];

    private String mogUser;

    private String mogGroup;

    private String name;

    private String description;

    private List<MogDataSource> dataSources = new ArrayList<>();

    private List<String> commandPaths = new ArrayList<>();

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

    public List<String> getCommandPaths() {
        return commandPaths;
    }

    public void setCommandPaths(List<String> commandPaths) {
        this.commandPaths = commandPaths;
    }

    public MogServerConfig getServer() {
        return serverConfig;
    }

    public void setServer(MogServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    public String toString() {
        return "MogConfig [serverConfig=" + serverConfig + ", environment=" + Arrays.toString(environment)
                + ", mogUser=" + mogUser + ", mogGroup=" + mogGroup + ", name=" + name + ", description=" + description
                + ", dataSources=" + dataSources + ", commandPaths=" + commandPaths + "]";
    }

}
