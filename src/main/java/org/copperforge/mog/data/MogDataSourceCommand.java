package org.copperforge.mog.data;

import org.copperforge.mog.MogException;
import org.copperforge.mog.annotations.MogCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MogCommand(name="datasource", description="MOG DataSource control and maintanence.")
public class MogDataSourceCommand implements Runnable {

    private Logger log = LoggerFactory.getLogger(MogDataSourceCommand.class);

    @Override
    public void run() {
        log.info("Command datasource, no sub-command");
    }

    @MogCommand(name="list", description="List the data sources available")
    public void list() throws MogException {
        log.info("Command datasource list");
    }
    
}
