package org.copperforge.mog.reporting;

import org.copperforge.mog.MogException;
import org.copperforge.mog.MogOptions;
import org.copperforge.mog.annotations.MogCommand;
import org.copperforge.mog.io.MogFileNameBuilder;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.definition.ReportService;
import org.copperforge.mog.reporting.writer.ReportWriter;
import org.copperforge.mog.reporting.writer.ReportWriterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MogCommand(name = "report", description = "Manage report definitions", method = "usage")
public class ReportingCommand {

    private Logger log = LoggerFactory.getLogger(ReportingCommand.class);

    public void usage(MogOptions options) throws MogException {
        log.info("mog Reporting");
        log.info("  Usage: mog report generate --report=<report-name>");
    }

    @MogCommand(name = "generate", description = "generate report")
    public void generate(MogOptions options) throws MogException {
        ReportOptions reportOptions = ReportOptions.parse(options);
        log.trace("reportOptions = " + reportOptions);

        Report definition = ReportService.instance().parse(reportOptions.getReport());
        log.info("Generating report " + definition.getName() + " ...");
        log.trace("report def = " + definition);
        ReportWriter builder = ReportWriterService.instance().builder(definition.getType());
        builder.build(definition);
        String filename = MogFileNameBuilder.build(definition.getFilename());
        builder.save(filename);
        log.info("Report can be found at '" + filename + "' ...");
    }

}
