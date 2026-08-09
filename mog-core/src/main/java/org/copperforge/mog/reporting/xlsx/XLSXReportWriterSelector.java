package org.copperforge.mog.reporting.xlsx;

import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.writer.ReportWriter;

public class XLSXReportWriterSelector implements ReportWriter {

    private ReportWriter selectedWriter;

    @Override
    public void build(Report definition) throws MogException {
        selectedWriter = selectWriter(definition);
        selectedWriter.build(definition);
    }

    @Override
    public void save(String filename) throws MogException {
        if (selectedWriter == null) {
            throw new MogException("The report must be built before it can be saved");
        }
        selectedWriter.save(filename);
    }

    public ReportWriter selectWriter(Report report) throws MogException {
        XLSXOptions options = options(report);
        return options.isStreaming() ? new StreamingXLSXReportWriter() : new XLSXReportWriter();
    }

    ReportWriter selectedWriter() {
        return selectedWriter;
    }

    private XLSXOptions options(Report report) {
        if (report instanceof XLSXReport xlsxReport) {
            return xlsxReport.getXlsx();
        }
        return new XLSXOptions();
    }
}
