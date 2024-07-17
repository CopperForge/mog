package org.copperforge.mog.archiving;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MogArchiveSet {

    private String name;

    private String archiveType;

    private String archivePath;

    private String archiveName;

    private List<String> assets;

    @JsonProperty("exclusions")
    @JsonAlias("exclude")
    private List<String> exclusions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArchiveType() {
        return archiveType;
    }

    public void setArchiveType(String archiveType) {
        this.archiveType = archiveType;
    }

    public String getArchivePath() {
        return archivePath;
    }

    public void setArchivePath(String archivePath) {
        this.archivePath = archivePath;
    }

    public String getArchiveName() {
        return archiveName;
    }

    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    public List<String> getAssets() {
        return assets;
    }

    public void setAssets(List<String> assets) {
        this.assets = assets;
    }

    public List<String> getExclusions() {
        return exclusions;
    }

    public void setExclusions(List<String> exclusions) {
        this.exclusions = exclusions;
    }

    @Override
    public String toString() {
        return "MogArchiveSet [name=" + name + ", archiveType=" + archiveType + ", archivePath=" + archivePath
                + ", archiveName=" + archiveName + ", assets=" + assets + ", exclusions=" + exclusions + "]";
    }
    

}
