package org.copperforge.mog.reporting.xlsx;

import java.io.FileOutputStream;

import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.definition.Sheet;
import org.copperforge.mog.reporting.element.ReportElement;
import org.copperforge.mog.reporting.element.SpannedText;
import org.copperforge.mog.reporting.element.table.Table;
import org.copperforge.mog.reporting.writer.AbstractReportWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamingXLSXReportWriter extends AbstractReportWriter {

    private final Logger log = LoggerFactory.getLogger(StreamingXLSXReportWriter.class);

    private SXSSFWorkbook workbook;
    private XLSXStyleCache styleCache;

    public StreamingXLSXReportWriter() {
        super("xlsx");
    }

    @Override
    protected void buildReport(Report report) throws MogException {
        XLSXOptions options = options(report);
        workbook = createWorkbook(options);
        styleCache = createStyleCache(report);
        try {
            validateStreamingReport(report);
            super.buildReport(report);
        } catch (MogException e) {
            closeWorkbook();
            throw e;
        } catch (Exception e) {
            closeWorkbook();
            throw new MogException(e);
        }
    }

    @Override
    protected void buildSheet(Report report, Sheet sheet) throws MogException {
        String title = sheet.getTitle();
        log.info("Building streaming sheet '" + title + "'");
        SXSSFSheet xlsxSheet = workbook.createSheet(title);
        for (ReportElement element : sheet.getElements()) {
            if ("table".equals(element.getType())) {
                new StreamingXLSXTableWriter().workbook(workbook).sheet(xlsxSheet).styles(styleCache)
                        .write(report, (Table) element);
            } else if ("spannedText".equals(element.getType())) {
                new StreamingXLSXSpannedTextWriter().sheet(xlsxSheet).styles(styleCache).write((SpannedText) element);
            } else {
                throw unsupported(report, sheet, element.getType(), "not supported in streaming mode");
            }
        }
    }

    @Override
    public void save(String filename) throws MogException {
        if (workbook == null) {
            throw new MogException("The report must be built before it can be saved");
        }
        try {
            try (FileOutputStream outputStream = new FileOutputStream(filename)) {
                workbook.write(outputStream);
            }
        } catch (Exception e) {
            throw new MogException(e);
        } finally {
            closeWorkbook();
        }
    }

    protected SXSSFWorkbook createWorkbook(XLSXOptions options) {
        return new SXSSFWorkbook(null, options.resolvedRowAccessWindowSize(),
                options.resolvedCompressTempFiles(), options.resolvedUseSharedStringsTable());
    }

    protected SXSSFWorkbook workbook() {
        return workbook;
    }

    protected void closeWorkbook() throws MogException {
        SXSSFWorkbook toClose = workbook;
        workbook = null;
        styleCache = null;
        if (toClose == null) {
            return;
        }
        try {
            toClose.close();
        } catch (Exception e) {
            throw new MogException(e);
        }
    }

    private XLSXOptions options(Report report) throws MogException {
        if (report instanceof XLSXReport xlsxReport) {
            XLSXOptions options = xlsxReport.getXlsx();
            options.isStreaming();
            return options;
        }
        return new XLSXOptions();
    }

    private XLSXStyleCache createStyleCache(Report report) {
        if (report instanceof XLSXReport xlsxReport) {
            return new XLSXStyleCache(workbook, xlsxReport.getStyles());
        }
        return new XLSXStyleCache(workbook, null);
    }

    private void validateStreamingReport(Report report) throws MogException {
        for (Sheet sheet : report.getSheets()) {
            for (ReportElement element : sheet.getElements()) {
                if (element == null) {
                    continue;
                }
                String type = element.getType();
                if (!"table".equals(type) && !"spannedText".equals(type)) {
                    throw unsupported(report, sheet, type, "not supported in streaming mode");
                }
            }
        }
    }

    private MogException unsupported(Report report, Sheet sheet, String type, String reason) {
        String reportName = report.getName() != null ? report.getName() : "<unnamed>";
        String sheetName = sheet.getTitle() != null ? sheet.getTitle() : sheet.getName();
        if (sheetName == null || sheetName.isBlank()) {
            sheetName = "<unnamed>";
        }
        return new MogException("XLSX streaming mode does not support element type '" + type
                + "' in report '" + reportName + "', sheet '" + sheetName + "': " + reason);
    }
}
