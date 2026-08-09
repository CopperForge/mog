package org.copperforge.mog.reporting.xlsx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.definition.CellReference;
import org.copperforge.mog.reporting.definition.Sheet;
import org.copperforge.mog.reporting.element.ReportElement;
import org.copperforge.mog.reporting.element.SpannedText;
import org.copperforge.mog.reporting.element.chart.Chart;
import org.copperforge.mog.reporting.element.table.PivotTable;
import org.copperforge.mog.reporting.element.table.Table;
import org.junit.jupiter.api.Test;

class StreamingXLSXReportWriterTest {

    @Test
    void emptyStreamingWorkbook_savesAndCanBeReopened() throws Exception {
        XLSXReport report = streamingReport("empty");
        report.setSheets(java.util.List.of(sheet("First"), sheet("Second")));
        Path file = Files.createTempFile("mog-streaming-empty", ".xlsx");

        try {
            StreamingXLSXReportWriter writer = new StreamingXLSXReportWriter();
            writer.build(report);
            writer.save(file.toString());

            try (XSSFWorkbook workbook = new XSSFWorkbook(file.toFile())) {
                assertEquals(2, workbook.getNumberOfSheets());
                assertNotNull(workbook.getSheet("First"));
                assertNotNull(workbook.getSheet("Second"));
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void workbookConfiguration_isAppliedWhereObservable() throws Exception {
        XLSXReport report = streamingReport("configured");
        XLSXOptions options = report.getXlsx();
        options.setRowAccessWindowSize(123);
        options.setCompressTempFiles(false);
        options.setUseSharedStringsTable(true);

        InspectingStreamingWriter writer = new InspectingStreamingWriter();
        writer.build(report);

        assertEquals(123, writer.createdWorkbook.getRandomAccessWindowSize());
        assertFalse(writer.createdWorkbook.isCompressTempFiles());
        assertTrue(hasSharedStringsSource(writer.createdWorkbook));

        Path file = Files.createTempFile("mog-streaming-config", ".xlsx");
        try {
            writer.save(file.toString());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void closeIsCalledAfterSuccessfulSave() throws Exception {
        XLSXReport report = streamingReport("cleanup-success");
        Path file = Files.createTempFile("mog-streaming-cleanup", ".xlsx");
        InspectingStreamingWriter writer = new InspectingStreamingWriter();

        try {
            writer.build(report);
            writer.save(file.toString());

            assertEquals(1, writer.closeCount);
            assertEquals(1, writer.createdWorkbook.closeCount);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void closeIsCalledAfterSaveFailure() throws Exception {
        XLSXReport report = streamingReport("cleanup-failure");
        Path directory = Files.createTempDirectory("mog-streaming-save-failure");
        InspectingStreamingWriter writer = new InspectingStreamingWriter();

        try {
            writer.build(report);

            assertThrows(MogException.class, () -> writer.save(directory.toString()));
            assertEquals(1, writer.closeCount);
            assertEquals(1, writer.createdWorkbook.closeCount);
        } finally {
            Files.deleteIfExists(directory);
        }
    }

    @Test
    void closeIsCalledAfterBuildValidationFailure() {
        XLSXReport report = streamingReport("cleanup-build-failure");
        report.setSheets(java.util.List.of(sheetWith("S", chart())));
        InspectingStreamingWriter writer = new InspectingStreamingWriter();

        assertThrows(MogException.class, () -> writer.build(report));

        assertEquals(1, writer.closeCount);
        assertEquals(1, writer.createdWorkbook.closeCount);
    }

    @Test
    void chartFailsClearlyInStreamingMode() {
        assertUnsupported(chart(), "chart", "streaming mode");
    }

    @Test
    void pivotFailsClearlyInStreamingMode() {
        assertUnsupported(pivot(), "pivotTable", "streaming mode");
    }

    @Test
    void tableFailsWithPhaseFiveMessage() {
        MogException ex = unsupported(table());

        assertTrue(ex.getMessage().contains("streaming mode"));
        assertTrue(ex.getMessage().contains("table"));
        assertTrue(ex.getMessage().contains("streaming table writer"));
    }

    @Test
    void spannedTextFailsClearlyUntilPhaseSix() {
        assertUnsupported(spannedText(), "spannedText", "streaming mode");
    }

    private void assertUnsupported(ReportElement element, String type, String context) {
        MogException ex = unsupported(element);

        assertTrue(ex.getMessage().contains(type));
        assertTrue(ex.getMessage().contains(context));
        assertTrue(ex.getMessage().contains("report 'unsupported'"));
        assertTrue(ex.getMessage().contains("sheet 'S'"));
    }

    private MogException unsupported(ReportElement element) {
        XLSXReport report = streamingReport("unsupported");
        report.setSheets(java.util.List.of(sheetWith("S", element)));
        StreamingXLSXReportWriter writer = new StreamingXLSXReportWriter();

        return assertThrows(MogException.class, () -> writer.build(report));
    }

    private XLSXReport streamingReport(String name) {
        XLSXReport report = new XLSXReport();
        report.setName(name);
        report.setType("xlsx");
        report.setFilename(name + ".xlsx");
        XLSXOptions options = new XLSXOptions();
        options.setMode("streaming");
        report.setXlsx(options);
        return report;
    }

    private Sheet sheet(String title) {
        Sheet sheet = new Sheet();
        sheet.setName(title);
        sheet.setTitle(title);
        return sheet;
    }

    private Sheet sheetWith(String title, ReportElement element) {
        Sheet sheet = sheet(title);
        sheet.setElements(java.util.List.of(element));
        return sheet;
    }

    private Chart chart() {
        Chart chart = new Chart();
        chart.setType("chart");
        return chart;
    }

    private PivotTable pivot() {
        PivotTable pivot = new PivotTable();
        pivot.setType("pivotTable");
        return pivot;
    }

    private Table table() {
        Table table = new Table();
        table.setType("table");
        return table;
    }

    private SpannedText spannedText() {
        SpannedText text = new SpannedText();
        text.setType("spannedText");
        CellReference upperLeft = new CellReference();
        upperLeft.setRow(1);
        upperLeft.setCol(1);
        text.setUpperLeft(upperLeft);
        return text;
    }

    private static boolean hasSharedStringsSource(SXSSFWorkbook workbook) throws Exception {
        Field field = SXSSFWorkbook.class.getDeclaredField("_sharedStringSource");
        field.setAccessible(true);
        return field.get(workbook) != null;
    }

    private static final class InspectingStreamingWriter extends StreamingXLSXReportWriter {
        private CountingSXSSFWorkbook createdWorkbook;
        private int closeCount;

        @Override
        protected SXSSFWorkbook createWorkbook(XLSXOptions options) {
            createdWorkbook = new CountingSXSSFWorkbook(options);
            return createdWorkbook;
        }

        @Override
        protected void closeWorkbook() throws MogException {
            closeCount++;
            super.closeWorkbook();
        }
    }

    private static final class CountingSXSSFWorkbook extends SXSSFWorkbook {
        private int closeCount;

        private CountingSXSSFWorkbook(XLSXOptions options) {
            super(null, options.resolvedRowAccessWindowSize(), options.resolvedCompressTempFiles(),
                    options.resolvedUseSharedStringsTable());
        }

        @Override
        public void close() throws java.io.IOException {
            closeCount++;
            super.close();
        }
    }
}
