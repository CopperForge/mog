package org.copperforge.mog.reporting;

import org.copperforge.mog.MogException;
import org.copperforge.mog.MogOptions;
import org.copperforge.mog.annotations.MogCommand;
import org.copperforge.mog.io.FileNameBuilder;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.definition.ReportService;
import org.copperforge.mog.reporting.writer.ReportWriter;
import org.copperforge.mog.reporting.writer.ReportWriterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MogCommand(name = "report", description = "Manage report definitions")
public class ReportingCommand {

    private Logger log = LoggerFactory.getLogger(ReportingCommand.class);

    @MogCommand(name = "generate", description = "generate report")
    public void generate(MogOptions options) throws MogException {
        ReportOptions reportOptions = ReportOptions.parse(options);
        log.trace("reportOptions = " + reportOptions);

        Report definition = ReportService.instance().parse(reportOptions.getReport());
        log.trace("report def = " + definition);
        ReportWriter builder = ReportWriterService.instance().builder(definition.getType());
        builder.build(definition);
        builder.save(FileNameBuilder.build(definition.getFilename()));
    }

}
