package org.copperforge.mog.data;

import org.copperforge.mog.Mog;
import org.copperforge.mog.MogException;
import org.copperforge.mog.annotations.MogCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MogCommand(name="datasource", description="MOG DataSource control and maintanence.")
public class MogDataSourceCommand {

    private Logger log = LoggerFactory.getLogger(MogDataSourceCommand.class);

    @MogCommand(name="list", description="List the data sources available")
    public void list() throws MogException {
        log.info("Command datasource list");
        log.info(Mog.mog().config().getDataSources().toString());
    }
    
}
