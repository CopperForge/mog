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

        // get the data
        MogDataSource mogDataSource = getDataSource(xlsxReport, tableElement);
        if (mogDataSource == null && log.isWarnEnabled()) {
            log.warn(String.format("Unable to determine datasource for table '%s'", tableElement.getName()));
        }

        List<? extends MogFetchable> data = (mogDataSource != null)
                ? mogDataSource.fetch(tableElement.getDataSource().getFilter())
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
        table.setDisplayName(tableElement.getTitle());

        setTableStyle(report, table, tableElement);

        // reusables
        int rowNum = tableElement.getUpperLeft().getRow();

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
                sheet().setColumnWidth(colNum + tableElement.getUpperLeft().getCol(), columnDef.getWidth() * 256);
            }

            if (log.isDebugEnabled())
                log.debug(String.format("table.getColumns() = %d; colNum = %d", table.getColumnCount(), colNum));

            column = table.getColumns().get(colNum);
            column.setName(columnDef.getTitle());
            cell = row.createCell(colNum++);
            cell.setCellValue(columnDef.getTitle());
        }
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
        CellReference topLeft = new CellReference(tableElement.getUpperLeft().getRow(),
                tableElement.getUpperLeft().getCol());
        CellReference bottomRight = new CellReference(tableElement.getUpperLeft().getRow() + rowCount,
                tableElement.getUpperLeft().getCol() + columnCount - 1);
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
        XSSFCell cell = row.createCell(colNum);

        if (columnDef.getKey() != null) {
            Object value = reportable.get(columnDef.getKey());

            if (value != null) {
                switch (value) {
                  case Long longValue -> {
                      cell.setCellValue(longValue);
                  }
                  case Integer intValue -> {
                      cell.setCellValue(intValue);
                  }
                  default -> {
                      cell.setCellValue(value.toString());
                  }
                }
            } else {
                cell.setCellValue("");
            }
        }
        return cell;
    }
}
