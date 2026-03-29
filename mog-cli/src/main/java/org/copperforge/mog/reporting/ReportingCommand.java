package org.copperforge.mog.reporting;

import org.copperforge.mog.Mog;
import org.copperforge.mog.MogException;
import org.copperforge.mog.MogOptions;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.runtime.MogRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(
    name = "report",
    description = "Report generation commands",
    subcommands = { ReportingCommand.Generate.class }
)
public class ReportingCommand {

    private static final Logger log = LoggerFactory.getLogger(ReportingCommand.class);

    @Command(name = "generate", description = "Generate a report from a definition")
    public static class Generate implements Runnable {

        @Mixin
        private ReportOptions reportOptions;

        @Override
        public void run() {
            try {
                MogOptions mogOptions = Mog.mog().options();
                ReportOptions options = reportOptions != null ? reportOptions : ReportOptions.parse(mogOptions);
                if (reportOptions.getReport() != null) {
                    options.setReport(reportOptions.getReport());
                }

                if (options.getReport() == null || options.getReport().isBlank()) {
                    log.info("Usage: mog report generate --report=<report-name>");
                    return;
                }

                log.trace("reportOptions = " + options);

                Report definition = MogRuntime.loadReportDefinition(options.getReport(), Mog.mog().context());
                log.info("Generating report " + definition.getName() + " ...");
                log.trace("report def = " + definition);
                String filename = MogRuntime.generateReport(definition, Mog.mog().context());
                log.info("Report can be found at '" + filename + "' ...");
            } catch (MogException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
