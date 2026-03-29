package org.copperforge.mog.reporting.xlsx;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFTableColumn;
import org.apache.poi.xssf.usermodel.XSSFTableStyleInfo;
import org.copperforge.mog.MogException;
import org.copperforge.mog.data.MogDataSource;
import org.copperforge.mog.data.MogFetchable;
import org.copperforge.mog.reporting.ReportDataSource;
import org.copperforge.mog.reporting.definition.Column;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.element.ReportElement;
import org.copperforge.mog.reporting.element.table.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XLSXTableWriter extends XLSXElementWriter<Table> {

    private Logger log = LoggerFactory.getLogger(XLSXTableWriter.class);

    @Override
    public void write(Report report, ReportElement element) throws MogException {
        if (log.isDebugEnabled())
            log.debug(String.format("Writing element %s", element));

        Table tableElement = (Table) element;
        XLSXReport xlsxReport = (XLSXReport) report;

        validateTableInputs(xlsxReport, tableElement);

        // get the data
        MogDataSource mogDataSource = getDataSource(xlsxReport, tableElement);
        if (tableElement.getDataSource() != null && mogDataSource == null) {
            // attempt to resolve from global catalog
            var name = tableElement.getDataSource().getName();
            mogDataSource = org.copperforge.mog.data.catalog.DataSourcesCatalog.instance()
                    .resolveByName(name, report.getContext());
            if (mogDataSource == null) {
                throw new MogException("Datasource '" + name + "' not found for table '" + tableElement.getName() + "'");
            }
        }

        List<? extends MogFetchable> data = (mogDataSource != null)
                ? mogDataSource.fetch(tableElement.getDataSource().getFilter(), report.getContext())
                : new ArrayList<>();

        List<Column> columns = tableElement.getColumns();
        int rowCount = (data != null && !data.isEmpty()) ? data.size() : 1;
        int columnCount = columns != null ? columns.size() : 0;

        if (log.isDebugEnabled())
            log.debug(String.format("rowCount = %d, columnCount = %d", rowCount, columnCount));

        AreaReference reference = getAreaReference(tableElement, columnCount, rowCount);
        XSSFTable table = writeTable(xlsxReport, tableElement, reference, columns, data);

        if (tableElement.getEnableFilters())
            table.getCTTable().addNewAutoFilter().setRef(reference.formatAsString());
    }

    void validateTableInputs(XLSXReport report, Table table) throws MogException {
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

    /**
     * 
     * @param report
     * @param tableElement
     * @return
     */
    MogDataSource getDataSource(XLSXReport report, Table tableElement) {
        ReportDataSource reportDataSource = tableElement.getDataSource();
        if (reportDataSource == null)
            return null;

        if (log.isTraceEnabled())
            log.trace(String.format("reportDataSource = %s", reportDataSource));

        return report.getDataSources().stream()
                .filter(d -> d.getName() != null && d.getName().equals(reportDataSource.getName())).findFirst()
                .orElse(null);
    }

    /**
     * 
     * @param tableElement
     * @param reference
     * @param columns
     * @param data
     */
    XSSFTable writeTable(XLSXReport report, Table tableElement, AreaReference reference, List<Column> columns,
            List<? extends MogFetchable> data) {
        XSSFTable table = sheet().createTable(reference);
        table.setName(tableElement.getName());
        table.setDisplayName(sanitizeDisplayName(tableElement.getTitle() != null ? tableElement.getTitle()
                : (tableElement.getName() != null ? tableElement.getName() : "Table")));

        setTableStyle(report, table, tableElement);

        // reusables
        int rowNum = tableElement.getUpperLeft().getRow() - 1; // convert 1-based to 0-based

        // headers
        if (columns != null) {
            writeColumnHeaders(table, tableElement, rowNum++, columns);
            writeRowData(tableElement, rowNum, columns, data);
        }

        return table;
    }

    /**
     * 
     * @param table
     * @param tableElement
     */
    void setTableStyle(XLSXReport report, XSSFTable table, Table tableElement) {
        String styleName = tableElement.getStyle();
        if (styleName == null)
            return;

        XLSXStyle xlsxStyle = report.getStyles().stream().filter(s -> s.getName().equals(styleName)).findFirst()
                .orElse(null);
        if (xlsxStyle instanceof XLSXTableStyle tableStyle) {
            table.getCTTable().addNewTableStyleInfo();
            table.getCTTable().getTableStyleInfo().setName(styleName);

            // Style the table
            XSSFTableStyleInfo style = (XSSFTableStyleInfo) table.getStyle();
            style.setName(styleName);
            style.setFirstColumn(tableStyle.getShowFirstColumn());
            style.setLastColumn(tableStyle.getShowLastColumn());
            style.setShowRowStripes(tableStyle.getShowRowStripes());
            style.setShowColumnStripes(tableStyle.getShowColumnStripes());
        }

    }

    /**
     * 
     */
    void writeColumnHeaders(XSSFTable table, Table tableElement, int rowNum, List<Column> columns) {
        XSSFRow row = sheet().createRow(rowNum);
        XSSFTableColumn column;
        XSSFCell cell;
        int colNum = 0;
        for (Column columnDef : columns) {
            if (columnDef.getWidth() != null) {
                sheet().setColumnWidth((tableElement.getUpperLeft().getCol() - 1) + colNum, columnDef.getWidth() * 256);
            }

            if (log.isDebugEnabled())
                log.debug(String.format("table.getColumns() = %d; colNum = %d", table.getColumnCount(), colNum));

            if (colNum >= table.getColumnCount()) {
                throw new RuntimeException("Header column count exceeds table width for table '" + tableElement.getName() + "'");
            }
            column = table.getColumns().get(colNum);
            column.setName(columnDef.getTitle());
            cell = row.createCell((tableElement.getUpperLeft().getCol() - 1) + colNum++);
            cell.setCellValue(columnDef.getTitle());
        }
    }

    String sanitizeDisplayName(String name) {
        if (name == null || name.isBlank()) return "Table";
        String sanitized = name.replaceAll("[^A-Za-z0-9_]", "_");
        if (!Character.isLetter(sanitized.charAt(0))) {
            sanitized = "T_" + sanitized;
        }
        return sanitized;
    }

    /**
     * 
     * @param tableElement
     * @param rowNum
     * @param columns
     * @param data
     */
    void writeRowData(Table tableElement, int rowNum, List<Column> columns, List<? extends MogFetchable> data) {
        if (data != null) {
            for (MogFetchable reportable : data) {
                writeRow(tableElement, rowNum++, columns, reportable);
            }
        } else {
            int columnCount = columns != null ? columns.size() : 0;
            XSSFCell cell;
            XSSFRow row = sheet().createRow(rowNum);
            for (int colNum = 0; colNum <= columnCount; colNum++) {
                cell = row.createCell(colNum);
                cell.setCellValue("");
            }
        }

    }

    /**
     * 
     * @param tableElement
     * @param columnCount
     * @param rowCount
     * @return
     */
    AreaReference getAreaReference(Table tableElement, int columnCount, int rowCount) {
        CellReference topLeft = new CellReference(tableElement.getUpperLeft().getRow() - 1,
                tableElement.getUpperLeft().getCol() - 1);
        CellReference bottomRight = new CellReference((tableElement.getUpperLeft().getRow() - 1) + rowCount,
                (tableElement.getUpperLeft().getCol() - 1) + columnCount - 1);
        return workbook().getCreationHelper().createAreaReference(topLeft, bottomRight);
    }

    /**
     * 
     * @param tableElement
     * @param rowNum
     * @param columns
     * @param reportable
     * @return
     */
    XSSFRow writeRow(Table tableElement, int rowNum, List<Column> columns, MogFetchable reportable) {
        XSSFRow row = sheet().createRow(rowNum);
        int colNum = tableElement.getUpperLeft().getCol();
        for (Column columnDef : columns) {
            writeColumn(row, colNum++, columnDef, reportable);
        }
        return row;
    }

    /**
     * 
     * @param row
     * @param colNum
     * @param columnDef
     * @param reportable
     * @return
     */
    XSSFCell writeColumn(XSSFRow row, int colNum, Column columnDef, MogFetchable reportable) {
        XSSFCell cell = row.createCell(colNum - 1); // convert 1-based to 0-based for column index

        if (columnDef.getKey() != null) {
            Object value = reportable.get(columnDef.getKey());

            if (value != null) {
                // Preserve numeric types so charts can read numeric ranges
                if (value instanceof Number num) {
                    if (num instanceof java.math.BigDecimal bd) {
                        cell.setCellValue(bd.doubleValue());
                    } else if (num instanceof java.math.BigInteger bi) {
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

        // Apply optional column-level style if provided
        if (columnDef.getStyle() != null) {
            cell.setCellStyle(columnDef.getStyle());
        }
        return cell;
    }
}
