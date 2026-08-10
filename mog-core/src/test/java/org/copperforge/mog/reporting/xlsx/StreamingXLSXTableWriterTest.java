package org.copperforge.mog.reporting.xlsx;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.copperforge.mog.MogException;
import org.copperforge.mog.data.MogDataSource;
import org.copperforge.mog.data.MogFetchCursor;
import org.copperforge.mog.data.MogFetchable;
import org.copperforge.mog.data.MogFetchCursors;
import org.copperforge.mog.data.MogJdbcDataSource;
import org.copperforge.mog.data.filter.MogDataFilter;
import org.copperforge.mog.data.filter.MogQueryFilter;
import org.copperforge.mog.reporting.ReportDataSource;
import org.copperforge.mog.reporting.definition.CellReference;
import org.copperforge.mog.reporting.definition.Column;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.definition.Sheet;
import org.copperforge.mog.reporting.element.ReportElement;
import org.copperforge.mog.reporting.element.SpannedText;
import org.copperforge.mog.reporting.element.table.Table;
import org.copperforge.mog.reporting.element.table.TableOverflow;
import org.copperforge.mog.runtime.MogContext;
import org.junit.jupiter.api.Test;

class StreamingXLSXTableWriterTest {

    @Test
    void streamingJdbcTable_writesHeaderFirstMiddleAndFinalRows() throws Exception {
        XLSXReport report = streamingReport(50);
        report.setDataSources(List.of(jdbcDataSource("jdbc:h2:mem:mog-stream-table-e2e;DB_CLOSE_DELAY=-1")));
        report.setSheets(List.of(sheetWith(table("t_numbers", "jdbc",
                "select x as id, 'name-' || x as name from system_range(1, 1000)"))));
        Path file = Files.createTempFile("mog-stream-table-e2e", ".xlsx");

        try {
            write(report, file);

            try (XSSFWorkbook workbook = new XSSFWorkbook(file.toFile())) {
                var sheet = workbook.getSheet("Numbers");
                assertNotNull(sheet);
                assertEquals("ID", sheet.getRow(0).getCell(0).getStringCellValue());
                assertEquals("Name", sheet.getRow(0).getCell(1).getStringCellValue());
                assertEquals(1.0, sheet.getRow(1).getCell(0).getNumericCellValue());
                assertEquals("name-1", sheet.getRow(1).getCell(1).getStringCellValue());
                assertEquals(500.0, sheet.getRow(500).getCell(0).getNumericCellValue());
                assertEquals("name-500", sheet.getRow(500).getCell(1).getStringCellValue());
                assertEquals(1000.0, sheet.getRow(1000).getCell(0).getNumericCellValue());
                assertEquals("name-1000", sheet.getRow(1000).getCell(1).getStringCellValue());
                assertEquals(1000, sheet.getLastRowNum());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void streamingTable_usesOpenCursorNotFetch() throws Exception {
        CursorOnlyDataSource dataSource = new CursorOnlyDataSource(rows(3));
        XLSXReport report = streamingReport(25);
        dataSource.setName("cursorOnly");
        report.setDataSources(List.of(dataSource));
        report.setSheets(List.of(sheetWith(table("t_cursor", "cursorOnly", null))));
        Path file = Files.createTempFile("mog-stream-cursor-only", ".xlsx");

        try {
            write(report, file);

            assertEquals(1, dataSource.openCursorCalls);
            try (XSSFWorkbook workbook = new XSSFWorkbook(file.toFile())) {
                var sheet = workbook.getSheet("Numbers");
                assertEquals(3.0, sheet.getRow(3).getCell(0).getNumericCellValue());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void streamingTable_survivesRowsBeyondSxssfWindow() throws Exception {
        XLSXReport report = streamingReport(25);
        report.setDataSources(List.of(jdbcDataSource("jdbc:h2:mem:mog-stream-table-window;DB_CLOSE_DELAY=-1")));
        report.setSheets(List.of(sheetWith(table("t_numbers", "jdbc",
                "select x as id, 'name-' || x as name from system_range(1, 2000)"))));
        Path file = Files.createTempFile("mog-stream-table-window", ".xlsx");

        try {
            write(report, file);

            try (XSSFWorkbook workbook = new XSSFWorkbook(file.toFile())) {
                var sheet = workbook.getSheet("Numbers");
                assertEquals(1.0, sheet.getRow(1).getCell(0).getNumericCellValue());
                assertEquals(1000.0, sheet.getRow(1000).getCell(0).getNumericCellValue());
                assertEquals(2000.0, sheet.getRow(2000).getCell(0).getNumericCellValue());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void emptyDatasource_writesHeaderOnlyAndHeaderAutofilter() throws Exception {
        CursorOnlyDataSource dataSource = new CursorOnlyDataSource(List.of());
        dataSource.setName("empty");
        XLSXReport report = streamingReport(25);
        report.setDataSources(List.of(dataSource));
        report.setSheets(List.of(sheetWith(table("t_empty", "empty", null))));
        Path file = Files.createTempFile("mog-stream-empty-table", ".xlsx");

        try {
            write(report, file);

            try (XSSFWorkbook workbook = new XSSFWorkbook(file.toFile())) {
                var sheet = workbook.getSheet("Numbers");
                assertEquals("ID", sheet.getRow(0).getCell(0).getStringCellValue());
                assertEquals("Name", sheet.getRow(0).getCell(1).getStringCellValue());
                assertNull(sheet.getRow(1));
                assertEquals("A1:B1", sheet.getCTWorksheet().getAutoFilter().getRef());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void autofilter_spansHeaderThroughFinalDetailRow() throws Exception {
        CursorOnlyDataSource dataSource = new CursorOnlyDataSource(rows(3));
        dataSource.setName("rows");
        XLSXReport report = streamingReport(25);
        report.setDataSources(List.of(dataSource));
        report.setSheets(List.of(sheetWith(table("t_rows", "rows", null))));
        Path file = Files.createTempFile("mog-stream-filter-table", ".xlsx");

        try {
            write(report, file);

            try (XSSFWorkbook workbook = new XSSFWorkbook(file.toFile())) {
                assertEquals("A1:B4", workbook.getSheet("Numbers").getCTWorksheet().getAutoFilter().getRef());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void rowLimit_allowsBoundaryAndRejectsOneOver() throws Exception {
        StreamingXLSXTableWriter writer = new StreamingXLSXTableWriter();
        Table table = table("t_limit", "none", null);
        table.getUpperLeft().setRow(1);

        long maxDetailRows = writer.maxDetailRows(table);

        assertEquals(StreamingXLSXTableWriter.MAX_WORKSHEET_ROWS - 1L, maxDetailRows);
        assertDoesNotThrow(() -> writer.checkDetailRowLimit(new Report(), table, maxDetailRows, maxDetailRows));

        MogException ex = assertThrows(MogException.class,
                () -> writer.checkDetailRowLimit(new Report(), table, maxDetailRows + 1, maxDetailRows));
        assertTrue(ex.getMessage().contains("maximum detail rows"));
        assertTrue(ex.getMessage().contains(String.valueOf(maxDetailRows)));
        assertTrue(ex.getMessage().contains(String.valueOf(StreamingXLSXTableWriter.MAX_WORKSHEET_ROWS + 1L)));
    }

    @Test
    void rowLimit_accountsForStartingHeaderRow() throws Exception {
        StreamingXLSXTableWriter writer = new StreamingXLSXTableWriter();
        Table table = table("t_limit", "none", null);
        table.getUpperLeft().setRow(StreamingXLSXTableWriter.MAX_WORKSHEET_ROWS);

        assertEquals(0, writer.maxDetailRows(table));

        MogException ex = assertThrows(MogException.class,
                () -> writer.checkDetailRowLimit(new Report(), table, 1, 0));
        assertTrue(ex.getMessage().contains("maximum detail rows for this placement is 0"));
    }

    @Test
    void writingFailure_closesCursorAndWorkbook() throws Exception {
        FailingCursorDataSource dataSource = new FailingCursorDataSource();
        dataSource.setName("failing");
        XLSXReport report = streamingReport(25);
        report.setDataSources(List.of(dataSource));
        report.setSheets(List.of(sheetWith(table("t_failing", "failing", null))));
        InspectingStreamingWriter writer = new InspectingStreamingWriter();

        assertThrows(MogException.class, () -> writer.build(report));

        assertTrue(dataSource.cursor.closed);
        assertEquals(1, writer.closeCount);
        assertEquals(1, writer.createdWorkbook.closeCount);
    }

    @Test
    void tableStyleFailsClearlyBecauseFormalXssfTableIsUnsupported() throws Exception {
        XLSXTableStyle style = new XLSXTableStyle();
        style.setType("table");
        style.setName("TableStyleLight12");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            StreamingXLSXTableWriter writer = new StreamingXLSXTableWriter()
                    .styles(new XLSXStyleCache(workbook, List.of(style)));
            Table table = table("t_styled", "none", null);
            table.setStyle("TableStyleLight12");

            MogException ex = assertThrows(MogException.class, () -> writer.validateStreamingTableFeatures(table));

            assertTrue(ex.getMessage().contains("streaming mode"));
            assertTrue(ex.getMessage().contains("table style"));
            assertTrue(ex.getMessage().contains("formal XSSFTable"));
        }
    }

    @Test
    void cellStyleNamedAsTableStyle_appliesToStreamingHeaderCells() throws Exception {
        CursorOnlyDataSource dataSource = new CursorOnlyDataSource(rows(2));
        dataSource.setName("rows");
        XLSXReport report = streamingReport(25);
        XLSXCellStyle style = new XLSXCellStyle();
        style.setType("cell");
        style.setName("headerStyle");
        style.setAlignment("center");
        style.setWrapText(true);
        report.setStyles(List.of(style));
        Table table = table("t_rows", "rows", null);
        table.setStyle("headerStyle");
        report.setDataSources(List.of(dataSource));
        report.setSheets(List.of(sheetWith(table)));
        Path file = Files.createTempFile("mog-stream-header-style", ".xlsx");

        try {
            write(report, file);

            try (XSSFWorkbook workbook = new XSSFWorkbook(file.toFile())) {
                var sheet = workbook.getSheet("Numbers");
                var idStyle = sheet.getRow(0).getCell(0).getCellStyle();
                var nameStyle = sheet.getRow(0).getCell(1).getCellStyle();
                assertEquals(HorizontalAlignment.CENTER, idStyle.getAlignment());
                assertTrue(idStyle.getWrapText());
                assertEquals(idStyle.getIndex(), nameStyle.getIndex());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void styleCache_reusesWorkbookCellStyleForRepeatedLookup() throws Exception {
        XLSXCellStyle style = new XLSXCellStyle();
        style.setType("cell");
        style.setName("reused");
        style.setAlignment("right");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XLSXStyleCache cache = new XLSXStyleCache(workbook, List.of(style));

            assertSame(cache.cellStyle("reused"), cache.cellStyle("reused"));
            assertEquals(2, workbook.getNumCellStyles());
        }
    }

    @Test
    void spannedTextBeforeLargeStreamingTable_preservesMergeAndRowsBeyondWindow() throws Exception {
        CursorOnlyDataSource dataSource = new CursorOnlyDataSource(rows(200));
        dataSource.setName("rows");
        XLSXReport report = streamingReport(25);
        report.setDataSources(List.of(dataSource));
        Table table = table("t_rows", "rows", null);
        table.getUpperLeft().setRow(3);
        report.setSheets(List.of(sheetWithElements("Numbers", spannedText(1, 1, 1, 2, "Sparse title"), table)));
        Path file = Files.createTempFile("mog-stream-spanned-table", ".xlsx");

        try {
            write(report, file);

            try (XSSFWorkbook workbook = new XSSFWorkbook(file.toFile())) {
                var sheet = workbook.getSheet("Numbers");
                assertEquals("Sparse title", sheet.getRow(0).getCell(0).getStringCellValue());
                assertEquals("A1:B1", sheet.getMergedRegion(0).formatAsString());
                assertEquals(200.0, sheet.getRow(202).getCell(0).getNumericCellValue());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void spannedTextAfterTargetRowsAreFlushed_failsClearlyAndClosesWorkbook() throws Exception {
        CursorOnlyDataSource dataSource = new CursorOnlyDataSource(rows(200));
        dataSource.setName("rows");
        XLSXReport report = streamingReport(25);
        report.setDataSources(List.of(dataSource));
        report.setSheets(List.of(sheetWithElements("Numbers", table("t_rows", "rows", null),
                spannedText(1, 1, 1, 2, "Too late"))));
        InspectingStreamingWriter writer = new InspectingStreamingWriter();

        MogException ex = assertThrows(MogException.class, () -> writer.build(report));

        assertTrue(ex.getMessage().contains("spanned text"));
        assertTrue(ex.getMessage().contains("flushed"));
        assertEquals(1, writer.closeCount);
        assertEquals(1, writer.createdWorkbook.closeCount);
    }

    @Test
    void overflowDisabled_datasetBelowLimit_writesOneSheetAndOneCursorLifecycle() throws Exception {
        CursorOnlyDataSource dataSource = new CursorOnlyDataSource(rows(4));
        dataSource.setName("rows");
        XLSXReport report = streamingReport(25);
        report.setDataSources(List.of(dataSource));
        Table table = table("t_rows", "rows", null);
        table.setOverflow(newSheetOverflow("Numbers %d", 5));
        report.setSheets(List.of(sheetWith(table)));
        Path file = Files.createTempFile("mog-stream-rollover-below", ".xlsx");

        try {
            write(report, file);

            assertEquals(1, dataSource.openCursorCalls);
            assertEquals(1, dataSource.closeCursorCalls);
            try (XSSFWorkbook workbook = new XSSFWorkbook(file.toFile())) {
                assertEquals(1, workbook.getNumberOfSheets());
                assertEquals(4.0, workbook.getSheet("Numbers").getRow(4).getCell(0).getNumericCellValue());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void overflowEnabled_datasetExactlyFillsAvailableRows_doesNotCreateContinuation() throws Exception {
        CursorOnlyDataSource dataSource = new CursorOnlyDataSource(rows(5));
        dataSource.setName("rows");
        XLSXReport report = streamingReport(25);
        report.setDataSources(List.of(dataSource));
        Table table = table("t_rows", "rows", null);
        table.setOverflow(newSheetOverflow("Numbers %d", 5));
        report.setSheets(List.of(sheetWith(table)));
        Path file = Files.createTempFile("mog-stream-rollover-exact", ".xlsx");

        try {
            write(report, file);

            try (XSSFWorkbook workbook = new XSSFWorkbook(file.toFile())) {
                assertEquals(1, workbook.getNumberOfSheets());
                assertEquals(5.0, workbook.getSheet("Numbers").getRow(5).getCell(0).getNumericCellValue());
                assertEquals("A1:B6", workbook.getSheet("Numbers").getCTWorksheet().getAutoFilter().getRef());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void overflowEnabled_oneRowOver_movesOverflowRowToFirstDetailRowOnContinuation() throws Exception {
        CursorOnlyDataSource dataSource = new CursorOnlyDataSource(rows(6));
        dataSource.setName("rows");
        XLSXReport report = streamingReport(25);
        report.setDataSources(List.of(dataSource));
        Table table = table("t_rows", "rows", null);
        table.setOverflow(newSheetOverflow("Numbers %d", 5));
        report.setSheets(List.of(sheetWith(table)));
        Path file = Files.createTempFile("mog-stream-rollover-one-over", ".xlsx");

        try {
            write(report, file);

            assertEquals(1, dataSource.openCursorCalls);
            assertEquals(1, dataSource.closeCursorCalls);
            try (XSSFWorkbook workbook = new XSSFWorkbook(file.toFile())) {
                assertEquals(2, workbook.getNumberOfSheets());
                assertEquals("Numbers", workbook.getSheetName(0));
                assertEquals("Numbers 2", workbook.getSheetName(1));
                assertEquals(5.0, workbook.getSheet("Numbers").getRow(5).getCell(0).getNumericCellValue());
                assertEquals(6.0, workbook.getSheet("Numbers 2").getRow(1).getCell(0).getNumericCellValue());
                assertNull(workbook.getSheet("Numbers 2").getRow(2));
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void overflowEnabled_multipleContinuationSheets_preservesContinuousOrderedValues() throws Exception {
        CursorOnlyDataSource dataSource = new CursorOnlyDataSource(rows(13));
        dataSource.setName("rows");
        XLSXReport report = streamingReport(25);
        report.setDataSources(List.of(dataSource));
        Table table = table("t_rows", "rows", null);
        table.setOverflow(newSheetOverflow("Numbers %d", 5));
        report.setSheets(List.of(sheetWith(table)));
        Path file = Files.createTempFile("mog-stream-rollover-many", ".xlsx");

        try {
            write(report, file);

            try (XSSFWorkbook workbook = new XSSFWorkbook(file.toFile())) {
                assertEquals(3, workbook.getNumberOfSheets());
                assertEquals(1.0, workbook.getSheet("Numbers").getRow(1).getCell(0).getNumericCellValue());
                assertEquals(5.0, workbook.getSheet("Numbers").getRow(5).getCell(0).getNumericCellValue());
                assertEquals(6.0, workbook.getSheet("Numbers 2").getRow(1).getCell(0).getNumericCellValue());
                assertEquals(10.0, workbook.getSheet("Numbers 2").getRow(5).getCell(0).getNumericCellValue());
                assertEquals(11.0, workbook.getSheet("Numbers 3").getRow(1).getCell(0).getNumericCellValue());
                assertEquals(13.0, workbook.getSheet("Numbers 3").getRow(3).getCell(0).getNumericCellValue());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void overflowEnabled_realJdbcDatasourceOpensOneCursorAcrossContinuationSheets() throws Exception {
        CountingJdbcDataSource dataSource = new CountingJdbcDataSource();
        dataSource.setName("jdbc");
        dataSource.setType("jdbc");
        dataSource.setUrl("jdbc:h2:mem:mog-stream-rollover-jdbc;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword(null);
        dataSource.setFetchSize(3);
        XLSXReport report = streamingReport(25);
        report.setDataSources(List.of(dataSource));
        Table table = table("t_jdbc", "jdbc", "select x, 'name-' || x as name from system_range(1, 13)");
        table.setOverflow(newSheetOverflow("Jdbc %d", 5));
        report.setSheets(List.of(sheetWith(table)));
        Path file = Files.createTempFile("mog-stream-rollover-jdbc", ".xlsx");

        try {
            write(report, file);

            assertEquals(1, dataSource.openCursorCalls);
            assertEquals(1, dataSource.closeCursorCalls);
            assertEquals(0, dataSource.fetchCalls);
            try (XSSFWorkbook workbook = new XSSFWorkbook(file.toFile())) {
                assertEquals(3, workbook.getNumberOfSheets());
                assertEquals(1.0, workbook.getSheet("Numbers").getRow(1).getCell(0).getNumericCellValue());
                assertEquals(6.0, workbook.getSheet("Jdbc 2").getRow(1).getCell(0).getNumericCellValue());
                assertEquals(11.0, workbook.getSheet("Jdbc 3").getRow(1).getCell(0).getNumericCellValue());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void overflowEnabled_repeatsHeadersWidthsStylesAndAutofilters() throws Exception {
        CursorOnlyDataSource dataSource = new CursorOnlyDataSource(rows(6));
        dataSource.setName("rows");
        XLSXReport report = streamingReport(25);
        XLSXCellStyle style = new XLSXCellStyle();
        style.setType("cell");
        style.setName("headerStyle");
        style.setAlignment("center");
        report.setStyles(List.of(style));
        report.setDataSources(List.of(dataSource));
        Table table = table("t_rows", "rows", null);
        table.setStyle("headerStyle");
        table.setOverflow(newSheetOverflow("Styled %d", 3));
        report.setSheets(List.of(sheetWith(table)));
        Path file = Files.createTempFile("mog-stream-rollover-style", ".xlsx");

        try {
            write(report, file);

            try (XSSFWorkbook workbook = new XSSFWorkbook(file.toFile())) {
                assertEquals(2, workbook.getNumberOfSheets());
                var first = workbook.getSheet("Numbers");
                var second = workbook.getSheet("Styled 2");
                assertEquals("ID", first.getRow(0).getCell(0).getStringCellValue());
                assertEquals("ID", second.getRow(0).getCell(0).getStringCellValue());
                assertEquals(first.getColumnWidth(0), second.getColumnWidth(0));
                assertEquals(first.getRow(0).getCell(0).getCellStyle().getAlignment(),
                        second.getRow(0).getCell(0).getCellStyle().getAlignment());
                assertEquals("A1:B4", first.getCTWorksheet().getAutoFilter().getRef());
                assertEquals("A1:B4", second.getCTWorksheet().getAutoFilter().getRef());
                assertTrue(workbook.getNumCellStyles() <= 3);
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void overflowEnabled_emptyDatasource_writesHeaderOnlyOriginalSheet() throws Exception {
        CursorOnlyDataSource dataSource = new CursorOnlyDataSource(List.of());
        dataSource.setName("empty");
        XLSXReport report = streamingReport(25);
        report.setDataSources(List.of(dataSource));
        Table table = table("t_empty", "empty", null);
        table.setOverflow(newSheetOverflow("Empty %d", 5));
        report.setSheets(List.of(sheetWith(table)));
        Path file = Files.createTempFile("mog-stream-rollover-empty", ".xlsx");

        try {
            write(report, file);

            try (XSSFWorkbook workbook = new XSSFWorkbook(file.toFile())) {
                assertEquals(1, workbook.getNumberOfSheets());
                assertEquals("A1:B1", workbook.getSheet("Numbers").getCTWorksheet().getAutoFilter().getRef());
                assertNull(workbook.getSheet("Numbers").getRow(1));
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void overflowEnabled_failureCreatingContinuation_closesCursorAndWorkbook() throws Exception {
        CursorOnlyDataSource dataSource = new CursorOnlyDataSource(rows(6));
        dataSource.setName("rows");
        XLSXReport report = streamingReport(25);
        report.setDataSources(List.of(dataSource));
        Table table = table("t_bad_name", "rows", null);
        table.setOverflow(newSheetOverflow("This worksheet name is far too long %d", 5));
        report.setSheets(List.of(sheetWith(table)));
        InspectingStreamingWriter writer = new InspectingStreamingWriter();

        MogException ex = assertThrows(MogException.class, () -> writer.build(report));

        assertTrue(ex.getMessage().contains("Invalid XLSX streaming continuation worksheet name"));
        assertEquals(1, dataSource.openCursorCalls);
        assertEquals(1, dataSource.closeCursorCalls);
        assertEquals(1, writer.closeCount);
        assertEquals(1, writer.createdWorkbook.closeCount);
    }

    @Test
    void overflowEnabled_duplicateContinuationNameFailsClearly() throws Exception {
        CursorOnlyDataSource dataSource = new CursorOnlyDataSource(rows(6));
        dataSource.setName("rows");
        XLSXReport report = streamingReport(25);
        report.setDataSources(List.of(dataSource));
        Table table = table("t_duplicate", "rows", null);
        table.setOverflow(newSheetOverflow("Numbers 2", 2));
        report.setSheets(List.of(sheetWith(table)));

        MogException ex = assertThrows(MogException.class, () -> new StreamingXLSXReportWriter().build(report));

        assertTrue(ex.getMessage().contains("duplicate worksheet name"));
    }

    @Test
    void overflowEnabled_invalidSheetNamePatternFailsClearly() throws Exception {
        CursorOnlyDataSource dataSource = new CursorOnlyDataSource(rows(6));
        dataSource.setName("rows");
        XLSXReport report = streamingReport(25);
        report.setDataSources(List.of(dataSource));
        Table table = table("t_invalid_pattern", "rows", null);
        table.setOverflow(newSheetOverflow("Numbers %q", 5));
        report.setSheets(List.of(sheetWith(table)));

        MogException ex = assertThrows(MogException.class, () -> new StreamingXLSXReportWriter().build(report));

        assertTrue(ex.getMessage().contains("invalid overflow sheetNamePattern"));
    }

    @Test
    void overflowEnabled_requiresTableOnlySourceSheet() throws Exception {
        CursorOnlyDataSource dataSource = new CursorOnlyDataSource(rows(6));
        dataSource.setName("rows");
        XLSXReport report = streamingReport(25);
        report.setDataSources(List.of(dataSource));
        Table table = table("t_rows", "rows", null);
        table.setOverflow(newSheetOverflow("Numbers %d", 5));
        report.setSheets(List.of(sheetWithElements("Numbers", spannedText(1, 1, 1, 2, "Title"), table)));

        MogException ex = assertThrows(MogException.class, () -> new StreamingXLSXReportWriter().build(report));

        assertTrue(ex.getMessage().contains("requires the table to be the only element"));
    }

    private void write(XLSXReport report, Path file) throws Exception {
        StreamingXLSXReportWriter writer = new StreamingXLSXReportWriter();
        writer.build(report);
        writer.save(file.toString());
    }

    private XLSXReport streamingReport(int rowWindow) {
        XLSXReport report = new XLSXReport();
        report.setName("streaming-table");
        report.setType("xlsx");
        XLSXOptions options = new XLSXOptions();
        options.setMode("streaming");
        options.setRowAccessWindowSize(rowWindow);
        report.setXlsx(options);
        return report;
    }

    private Sheet sheetWith(Table table) {
        Sheet sheet = new Sheet();
        sheet.setName("Numbers");
        sheet.setTitle("Numbers");
        sheet.setElements(List.of(table));
        return sheet;
    }

    private Sheet sheetWithElements(String title, ReportElement... elements) {
        Sheet sheet = new Sheet();
        sheet.setName(title);
        sheet.setTitle(title);
        sheet.setElements(List.of(elements));
        return sheet;
    }

    private Table table(String name, String datasourceName, String query) throws Exception {
        Table table = new Table();
        table.setType("table");
        table.setName(name);
        CellReference upperLeft = new CellReference();
        upperLeft.setRow(1);
        upperLeft.setCol(1);
        table.setUpperLeft(upperLeft);
        table.setColumns(List.of(new Column("ID", "x", 12), new Column("Name", "name", 20)));
        ReportDataSource reportDataSource = new ReportDataSource();
        reportDataSource.setName(datasourceName);
        if (query != null) {
            MogQueryFilter filter = new MogQueryFilter(query);
            filter.setType("query");
            reportDataSource.setFilter(filter);
        } else {
            reportDataSource.setFilter(new MogDataFilter());
        }
        table.setDataSource(reportDataSource);
        return table;
    }

    private SpannedText spannedText(int firstRow, int firstCol, int lastRow, int lastCol, String text) {
        SpannedText spannedText = new SpannedText();
        spannedText.setType("spannedText");
        CellReference upperLeft = new CellReference();
        upperLeft.setRow(firstRow);
        upperLeft.setCol(firstCol);
        spannedText.setUpperLeft(upperLeft);
        CellReference lowerRight = new CellReference();
        lowerRight.setRow(lastRow);
        lowerRight.setCol(lastCol);
        spannedText.setLowerRight(lowerRight);
        spannedText.setText(text);
        return spannedText;
    }

    private TableOverflow newSheetOverflow(String sheetNamePattern, long maxDetailRowsPerSheet) {
        TableOverflow overflow = new TableOverflow();
        overflow.setMode("newSheet");
        overflow.setSheetNamePattern(sheetNamePattern);
        overflow.setMaxDetailRowsPerSheet(maxDetailRowsPerSheet);
        return overflow;
    }

    private MogJdbcDataSource jdbcDataSource(String url) {
        MogJdbcDataSource dataSource = new MogJdbcDataSource();
        dataSource.setName("jdbc");
        dataSource.setType("jdbc");
        dataSource.setUrl(url);
        dataSource.setUser("sa");
        dataSource.setPassword(null);
        return dataSource;
    }

    private List<MogFetchable> rows(int count) {
        List<MogFetchable> rows = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            rows.add(new MogFetchable(Map.of("x", i, "name", "name-" + i)));
        }
        return rows;
    }

    private static final class CursorOnlyDataSource extends MogDataSource {
        private final List<MogFetchable> rows;
        private int openCursorCalls;
        private int closeCursorCalls;

        private CursorOnlyDataSource(List<MogFetchable> rows) {
            this.rows = rows;
        }

        @Override
        public List<MogFetchable> fetch(MogDataFilter filter, MogContext context) throws MogException {
            throw new MogException("fetch must not be called");
        }

        @Override
        public MogFetchCursor openCursor(MogDataFilter filter, MogContext context) {
            openCursorCalls++;
            MogFetchCursor delegate = MogFetchCursors.fromList(rows);
            return new MogFetchCursor() {
                private boolean closed = false;

                @Override
                public List<String> columns() throws MogException {
                    return delegate.columns();
                }

                @Override
                public boolean next() throws MogException {
                    return delegate.next();
                }

                @Override
                public MogFetchable current() throws MogException {
                    return delegate.current();
                }

                @Override
                public long rowNumber() {
                    return delegate.rowNumber();
                }

                @Override
                public void close() throws MogException {
                    if (closed) {
                        return;
                    }
                    closed = true;
                    try {
                        delegate.close();
                    } finally {
                        closeCursorCalls++;
                    }
                }
            };
        }
    }

    private static final class FailingCursorDataSource extends MogDataSource {
        private final FailingCursor cursor = new FailingCursor();

        @Override
        public List<MogFetchable> fetch(MogDataFilter filter, MogContext context) throws MogException {
            throw new MogException("fetch must not be called");
        }

        @Override
        public MogFetchCursor openCursor(MogDataFilter filter, MogContext context) {
            return cursor;
        }
    }

    private static final class CountingJdbcDataSource extends MogJdbcDataSource {
        private int openCursorCalls;
        private int closeCursorCalls;
        private int fetchCalls;

        @Override
        public List<MogFetchable> fetch(MogDataFilter filter, MogContext context) throws MogException {
            fetchCalls++;
            throw new MogException("fetch must not be called");
        }

        @Override
        public MogFetchCursor openCursor(MogDataFilter filter, MogContext context) throws MogException {
            openCursorCalls++;
            MogFetchCursor delegate = super.openCursor(filter, context);
            return new MogFetchCursor() {
                private boolean closed = false;

                @Override
                public List<String> columns() throws MogException {
                    return delegate.columns();
                }

                @Override
                public boolean next() throws MogException {
                    return delegate.next();
                }

                @Override
                public MogFetchable current() throws MogException {
                    return delegate.current();
                }

                @Override
                public long rowNumber() {
                    return delegate.rowNumber();
                }

                @Override
                public void close() throws MogException {
                    if (closed) {
                        return;
                    }
                    closed = true;
                    try {
                        delegate.close();
                    } finally {
                        closeCursorCalls++;
                    }
                }
            };
        }
    }

    private static final class FailingCursor implements MogFetchCursor {
        private int nextCalls;
        private boolean closed;

        @Override
        public List<String> columns() {
            return List.of("x", "name");
        }

        @Override
        public boolean next() {
            return ++nextCalls <= 3;
        }

        @Override
        public MogFetchable current() {
            if (nextCalls == 3) {
                return new MogFetchable(Map.of("x", 3, "name", new FailingToString()));
            }
            return new MogFetchable(Map.of("x", nextCalls, "name", "name-" + nextCalls));
        }

        @Override
        public long rowNumber() {
            return nextCalls;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class FailingToString {
        @Override
        public String toString() {
            throw new IllegalStateException("cannot stringify");
        }
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
