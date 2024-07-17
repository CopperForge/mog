package org.copperforge.mog.data;

import java.util.List;

import org.copperforge.mog.annotations.MogService;
import org.copperforge.mog.annotations.Moglet;
import org.copperforge.mog.config.MogConfig;

@MogService(name = "dataSourceService")
public class MogDataSourceService {

    @Moglet
    MogConfig config;

    public List<MogDataSource> list() {
        return config.getDataSources();
    }
}
