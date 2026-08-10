package org.copperforge.mog.reporting.xlsx;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.copperforge.mog.MogException;
import org.copperforge.mog.data.MogDataSource;
import org.copperforge.mog.data.MogFetchCursor;
import org.copperforge.mog.data.MogFetchable;
import org.copperforge.mog.data.catalog.DataSourcesCatalog;
import org.copperforge.mog.reporting.ReportDataSource;
import org.copperforge.mog.reporting.definition.Column;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.element.table.Table;
import org.copperforge.mog.reporting.element.table.TableOverflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamingXLSXTableWriter {

    static final int MAX_WORKSHEET_ROWS = SpreadsheetVersion.EXCEL2007.getMaxRows();

    private final Logger log = LoggerFactory.getLogger(StreamingXLSXTableWriter.class);

    private SXSSFWorkbook workbook;
    private SXSSFSheet sheet;
    private XLSXStyleCache styles;
    private StreamingXLSXSheetFactory sheetFactory;
    private String sourceSheetName;
    private int continuationSheetNumber;

    public StreamingXLSXTableWriter workbook(SXSSFWorkbook workbook) {
        this.workbook = workbook;
        return this;
    }

    public StreamingXLSXTableWriter sheet(SXSSFSheet sheet) {
        this.sheet = sheet;
        return this;
    }

    public StreamingXLSXTableWriter styles(XLSXStyleCache styles) {
        this.styles = styles;
        return this;
    }

    public StreamingXLSXTableWriter sheetFactory(StreamingXLSXSheetFactory sheetFactory) {
        this.sheetFactory = sheetFactory;
        return this;
    }

    public void write(Report report, Table tableElement) throws MogException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Writing streaming table %s", tableElement));
        }

        validateTableInputs(tableElement);
        validateStreamingTableFeatures(tableElement);

        List<Column> columns = tableElement.getColumns();
        int headerRowIndex = tableElement.getUpperLeft().getRow() - 1;
        int firstColIndex = tableElement.getUpperLeft().getCol() - 1;
        int lastColIndex = firstColIndex + columns.size() - 1;
        sourceSheetName = sheet.getSheetName();
        continuationSheetNumber = 1;

        long maxDetailRows = effectiveMaxDetailRows(tableElement);
        writeColumnHeaders(tableElement, headerRowIndex, columns);

        long currentSheetRows = 0;
        MogDataSource dataSource = resolveDataSource(report, tableElement);
        if (dataSource != null) {
            ReportDataSource reportDataSource = tableElement.getDataSource();
            try (MogFetchCursor cursor = dataSource.openCursor(reportDataSource.getFilter(), report.getContext())) {
                while (cursor.next()) {
                    if (currentSheetRows >= maxDetailRows) {
                        if (!isNewSheetOverflow(tableElement)) {
                            checkDetailRowLimit(report, tableElement, currentSheetRows + 1, maxDetailRows);
                        }
                        finalizeAutofilter(tableElement, currentSheetRows, headerRowIndex, firstColIndex, lastColIndex);
                        sheet = createContinuationSheet(tableElement, columns);
                        currentSheetRows = 0;
                    }
                    currentSheetRows++;
                    writeRow(tableElement, headerRowIndex + (int) currentSheetRows, columns, cursor.current());
                }
            }
        }

        finalizeAutofilter(tableElement, currentSheetRows, headerRowIndex, firstColIndex, lastColIndex);
    }

    void validateTableInputs(Table table) throws MogException {
        if (table.getUpperLeft() == null || table.getUpperLeft().getRow() == null
                || table.getUpperLeft().getCol() == null) {
            throw new MogException("Table '" + table.getName() + "' requires upperLeft row and col (1-based)");
        }
        if (table.getUpperLeft().getRow() < 1 || table.getUpperLeft().getCol() < 1) {
            throw new MogException("Table '" + table.getName() + "' coordinates must be >= 1");
        }
        if (table.getColumns() == null || table.getColumns().isEmpty()) {
            throw new MogException("Table '" + table.getName() + "' requires at least one column");
        }
    }

    void validateStreamingTableFeatures(Table table) throws MogException {
        if (table.getStyle() != null && !table.getStyle().isBlank()
                && styles != null && styles.style(table.getStyle()) instanceof XLSXTableStyle) {
            throw new MogException("XLSX streaming mode does not support table style '" + table.getStyle()
                    + "' for table '" + table.getName() + "' because formal XSSFTable styling is not available");
        }
        TableOverflow overflow = table.getOverflow();
        if (overflow == null || overflow.getMode() == null || overflow.getMode().isBlank()) {
            return;
        }
        if (!overflow.isNewSheet()) {
            throw new MogException("XLSX streaming table '" + table.getName()
                    + "' has unsupported overflow mode '" + overflow.getMode() + "'");
        }
        formatContinuationSheetName(table, 2);
    }

    long maxDetailRows(Table table) throws MogException {
        int headerRow = table.getUpperLeft().getRow();
        if (headerRow > MAX_WORKSHEET_ROWS) {
            throw new MogException("Table '" + table.getName() + "' header row " + headerRow
                    + " exceeds XLSX worksheet row limit " + MAX_WORKSHEET_ROWS);
        }
        return MAX_WORKSHEET_ROWS - headerRow;
    }

    long effectiveMaxDetailRows(Table table) throws MogException {
        long maxDetailRows = maxDetailRows(table);
        TableOverflow overflow = table.getOverflow();
        Long configured = overflow != null ? overflow.getMaxDetailRowsPerSheet() : null;
        if (configured == null) {
            if (isNewSheetOverflow(table) && maxDetailRows < 1) {
                throw new MogException("XLSX streaming table '" + table.getName()
                        + "' cannot overflow because the table placement leaves no detail rows on a worksheet");
            }
            return maxDetailRows;
        }
        if (configured < 1) {
            throw new MogException("XLSX streaming table '" + table.getName()
                    + "' overflow maxDetailRowsPerSheet must be >= 1");
        }
        if (configured > maxDetailRows) {
            throw new MogException("XLSX streaming table '" + table.getName()
                    + "' overflow maxDetailRowsPerSheet " + configured
                    + " exceeds the XLSX worksheet detail row limit " + maxDetailRows);
        }
        return configured;
    }

    void checkDetailRowLimit(Report report, Table table, long rowsWritten, long maxDetailRows) throws MogException {
        if (rowsWritten <= maxDetailRows) {
            return;
        }
        String sheetName = sheet != null ? sheet.getSheetName() : "<unknown>";
        String reportName = report != null && report.getName() != null ? report.getName() : "<unnamed>";
        long overflowRow = table.getUpperLeft().getRow() + rowsWritten;
        throw new MogException("XLSX streaming table '" + table.getName() + "' in report '" + reportName
                + "', sheet '" + sheetName + "' exceeds worksheet row limit " + MAX_WORKSHEET_ROWS
                + "; maximum detail rows for this placement is " + maxDetailRows
                + "; overflow occurred at worksheet row " + overflowRow);
    }

    MogDataSource resolveDataSource(Report report, Table tableElement) throws MogException {
        ReportDataSource reportDataSource = tableElement.getDataSource();
        if (reportDataSource == null) {
            return null;
        }

        MogDataSource dataSource = report.getDataSources().stream()
                .filter(d -> d.getName() != null && d.getName().equals(reportDataSource.getName()))
                .findFirst()
                .orElse(null);

        if (dataSource == null) {
            String name = reportDataSource.getName();
            dataSource = DataSourcesCatalog.instance().resolveByName(name, report.getContext());
            if (dataSource == null) {
                throw new MogException("Datasource '" + name + "' not found for table '" + tableElement.getName() + "'");
            }
        }
        return dataSource;
    }

    void writeColumnHeaders(Table tableElement, int rowNum, List<Column> columns) throws MogException {
        Row row = sheet.createRow(rowNum);
        int colNum = tableElement.getUpperLeft().getCol() - 1;
        CellStyle headerStyle = styles != null ? styles.cellStyle(tableElement.getStyle()) : null;
        for (Column columnDef : columns) {
            if (columnDef.getWidth() != null) {
                sheet.setColumnWidth(colNum, columnDef.getWidth() * 256);
            }
            Cell cell = row.createCell(colNum++);
            if (headerStyle != null) {
                cell.setCellStyle(headerStyle);
            }
            cell.setCellValue(columnDef.getTitle());
        }
    }

    Row writeRow(Table tableElement, int rowNum, List<Column> columns, MogFetchable reportable) {
        Row row = sheet.createRow(rowNum);
        int colNum = tableElement.getUpperLeft().getCol();
        for (Column columnDef : columns) {
            writeColumn(row, colNum++, columnDef, reportable);
        }
        return row;
    }

    Cell writeColumn(Row row, int colNum, Column columnDef, MogFetchable reportable) {
        Cell cell = row.createCell(colNum - 1);
        if (columnDef.getStyle() != null) {
            cell.setCellStyle(columnDef.getStyle());
        }

        if (columnDef.getKey() != null) {
            Object value = reportable.get(columnDef.getKey());
            if (value != null) {
                if (value instanceof Number num) {
                    if (num instanceof BigDecimal bd) {
                        cell.setCellValue(bd.doubleValue());
                    } else if (num instanceof BigInteger bi) {
                        cell.setCellValue(bi.doubleValue());
                    } else {
                        cell.setCellValue(num.doubleValue());
                    }
                } else if (value instanceof Boolean b) {
                    cell.setCellValue(b);
                } else {
                    cell.setCellValue(value.toString());
                }
            } else {
                cell.setCellValue("");
            }
        }

        return cell;
    }

    void finalizeAutofilter(Table tableElement, long rowsWritten, int headerRowIndex, int firstColIndex,
            int lastColIndex) {
        if (tableElement.getEnableFilters()) {
            int lastRowIndex = headerRowIndex + (int) rowsWritten;
            sheet.setAutoFilter(new CellRangeAddress(headerRowIndex, lastRowIndex, firstColIndex, lastColIndex));
        }
    }

    SXSSFSheet createContinuationSheet(Table tableElement, List<Column> columns) throws MogException {
        if (sheetFactory == null) {
            throw new MogException("XLSX streaming table overflow requires a continuation sheet factory");
        }
        int continuationNumber = ++continuationSheetNumber;
        String sheetName = formatContinuationSheetName(tableElement, continuationNumber);
        SXSSFSheet continuation = sheetFactory.createContinuationSheet(sheetName);
        SXSSFSheet previous = sheet;
        sheet = continuation;
        try {
            writeColumnHeaders(tableElement, tableElement.getUpperLeft().getRow() - 1, columns);
            return continuation;
        } catch (MogException e) {
            sheet = previous;
            throw e;
        }
    }

    String formatContinuationSheetName(Table tableElement, int continuationNumber) throws MogException {
        TableOverflow overflow = tableElement.getOverflow();
        String pattern = overflow != null ? overflow.getSheetNamePattern() : null;
        if (pattern == null || pattern.isBlank()) {
            String baseName = sourceSheetName != null ? sourceSheetName : (sheet != null ? sheet.getSheetName() : "Sheet");
            pattern = baseName + " %d";
        }
        try {
            String formatted = String.format(Locale.ROOT, pattern, continuationNumber);
            if (formatted == null || formatted.isBlank()) {
                throw new MogException("XLSX streaming table '" + tableElement.getName()
                        + "' overflow sheetNamePattern produced a blank worksheet name");
            }
            return formatted;
        } catch (IllegalFormatException e) {
            throw new MogException("XLSX streaming table '" + tableElement.getName()
                    + "' has invalid overflow sheetNamePattern '" + pattern + "': " + e.getMessage(), e);
        }
    }

    private boolean isNewSheetOverflow(Table tableElement) {
        return tableElement.getOverflow() != null && tableElement.getOverflow().isNewSheet();
    }
}
