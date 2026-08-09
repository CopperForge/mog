package org.copperforge.mog.reporting.xlsx;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.copperforge.mog.reporting.writer.ReportWriterService;
import org.junit.jupiter.api.Test;

class XLSXReportWriterSelectorTest {

    @Test
    void serviceBuilder_returnsSelectorForXlsxType() {
        assertInstanceOf(XLSXReportWriterSelector.class, ReportWriterService.instance().builder("xlsx"));
    }

    @Test
    void normalMode_selectsExistingXssfWriter() throws Exception {
        XLSXReport report = reportWithMode("normal");
        XLSXReportWriterSelector selector = new XLSXReportWriterSelector();

        assertInstanceOf(XLSXReportWriter.class, selector.selectWriter(report));
    }

    @Test
    void streamingMode_selectsStreamingWriter() throws Exception {
        XLSXReport report = reportWithMode("streaming");
        XLSXReportWriterSelector selector = new XLSXReportWriterSelector();

        assertInstanceOf(StreamingXLSXReportWriter.class, selector.selectWriter(report));
    }

    private XLSXReport reportWithMode(String mode) {
        XLSXReport report = new XLSXReport();
        report.setType("xlsx");
        XLSXOptions options = new XLSXOptions();
        options.setMode(mode);
        report.setXlsx(options);
        return report;
    }
}
