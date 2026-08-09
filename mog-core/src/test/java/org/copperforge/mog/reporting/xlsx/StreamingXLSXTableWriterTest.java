package org.copperforge.mog.reporting.xlsx;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.copperforge.mog.reporting.element.table.Table;
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
        StreamingXLSXTableWriter writer = new StreamingXLSXTableWriter();
        Table table = table("t_styled", "none", null);
        table.setStyle("TableStyleLight12");

        MogException ex = assertThrows(MogException.class, () -> writer.validateStreamingTableFeatures(table));

        assertTrue(ex.getMessage().contains("streaming mode"));
        assertTrue(ex.getMessage().contains("table style"));
        assertTrue(ex.getMessage().contains("formal XSSFTable"));
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

        private CursorOnlyDataSource(List<MogFetchable> rows) {
            this.rows = rows;
        }

        @Override
        public List<? extends MogFetchable> fetch(MogDataFilter filter, MogContext context) throws MogException {
            throw new MogException("fetch must not be called");
        }

        @Override
        public MogFetchCursor openCursor(MogDataFilter filter, MogContext context) {
            openCursorCalls++;
            return MogFetchCursors.fromList(rows);
        }
    }

    private static final class FailingCursorDataSource extends MogDataSource {
        private final FailingCursor cursor = new FailingCursor();

        @Override
        public List<? extends MogFetchable> fetch(MogDataFilter filter, MogContext context) throws MogException {
            throw new MogException("fetch must not be called");
        }

        @Override
        public MogFetchCursor openCursor(MogDataFilter filter, MogContext context) {
            return cursor;
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
