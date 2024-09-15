package org.copperforge.mog.data;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.copperforge.mog.Mog;
import org.copperforge.mog.MogException;
import org.copperforge.mog.MogOptions;
import org.copperforge.mog.annotations.MogCommand;
import org.copperforge.mog.command.MogCommandResponse;
import org.copperforge.mog.data.filter.MogQueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Option;

@MogCommand
public class MogDataCommands {

    private Logger log = LoggerFactory.getLogger(MogDataCommands.class);

    @Option(names = { "--where", "--query" })
    private String query = null;

    @Option(names = { "--datasource" })
    private String dataSourceName;

    @MogCommand(name = "query")
    public MogCommandResponse query(MogOptions options) throws MogException {
        log.debug("options = " + options);

        new CommandLine(this).setUnmatchedArgumentsAllowed(true)
                .parseArgs(options.rawArgs().toArray(new String[0]));

        log.debug("dataSourceName = " + dataSourceName);
        log.debug("query = " + query);

        MogDataSource dataSource = Mog.mog().config().getDataSources().stream()
                .filter(d -> d.getName().equals(dataSourceName)).findFirst().orElse(null);

        MogQueryFilter filter = null;
        final List<String> columns = new ArrayList<>(Arrays.asList("*"));
        if (query != null) {
            filter = new MogQueryFilter(query);
            log.debug("columns = " + filter.columns());
            columns.clear();
            columns.addAll(filter.columns());
        }
        List<? extends MogFetchable> fetched = dataSource.fetch(filter);
        log.debug("fetched = " + fetched);

        int i = 1;
        for (MogFetchable record : fetched) {
            Set<String> fieldNames = record.keys();
            if (columns.contains("*")) {
                columns.clear();
                columns.addAll(fieldNames);
            }
            if (i == 1) {
                // print the header
                System.out.println(columns.stream()
                        .map(f -> "\"" + f + "\"")
                        .collect(Collectors.joining(",")));
            }

            // print the row
            System.out.println(i + " :: "
                    + columns.stream()
                            .map(f -> "\"" + record.get(f).toString() + "\"")
                            .collect(Collectors.joining(",")));

            i++;
        }

        MogCommandResponse response = new MogCommandResponse();
        response.setResponse(new ByteArrayInputStream("queried".getBytes()));
        return response;
    }

}
