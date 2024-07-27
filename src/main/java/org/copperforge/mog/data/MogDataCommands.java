package org.copperforge.mog.data;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.copperforge.mog.Mog;
import org.copperforge.mog.MogException;
import org.copperforge.mog.MogOptions;
import org.copperforge.mog.annotations.MogCommand;
import org.copperforge.mog.command.MogCommandResponse;
import org.copperforge.mog.data.filter.MogDataFilter;
import org.copperforge.mog.data.filter.MogQueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Option;

@MogCommand
public class MogDataCommands {

    private Logger log = LoggerFactory.getLogger(MogDataCommands.class);

    @Option(names = { "--where" })
    private String where = null;

    @Option(names = { "--datasource" })
    private String dataSourceName;

    @MogCommand(name = "query")
    public MogCommandResponse query(MogOptions options) throws MogException {
        log.info("query = " + options);

        new CommandLine(this).setUnmatchedArgumentsAllowed(true)
                .parseArgs(options.rawArgs().toArray(new String[0]));

        log.info("dataSourceName = " + dataSourceName);
        log.info("where = " + where);

        MogDataSource dataSource = Mog.mog().config().getDataSources().stream()
                .filter(d -> d.getName().equals(dataSourceName)).findFirst().orElse(null);

        MogDataFilter filter = null;
        if (where != null) filter = new MogQueryFilter(where);
        List<? extends MogFetchable> fetched = dataSource.fetch(filter);
        log.debug("fetched = " + fetched);

        int i = 1;
        for (MogFetchable record : fetched) {
            Set<String> fieldNames = record.keys();
            if (i == 1) {
                // print the header
                System.out.println(String.join(",", fieldNames));
            }

            // print the row
            String value = fieldNames.stream().map(f -> "\"" + record.get(f).toString() + "\"").collect(Collectors.joining(","));
            System.out.println(i + " :: " + value);

            i++;
        }

        MogCommandResponse response = new MogCommandResponse();
        response.setResponse(new ByteArrayInputStream("queried".getBytes()));
        return response;
    }

}
