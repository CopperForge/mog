package org.copperforge.mog.data.catalog;

import java.util.ArrayList;
import java.util.List;

import org.copperforge.mog.data.MogDataSource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataSourcesFile {

    private final List<MogDataSource> datasources = new ArrayList<>();

    public List<MogDataSource> getDatasources() {
        return datasources;
    }

    public void setDatasources(List<MogDataSource> ds) {
        this.datasources.clear();
        if (ds != null) this.datasources.addAll(ds);
    }
}

