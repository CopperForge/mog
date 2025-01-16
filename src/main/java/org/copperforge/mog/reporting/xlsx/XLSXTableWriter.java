package org.copperforge.mog.reporting.xlsx;

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

        // get the data
        MogDataSource mogDataSource = getDataSource(report, tableElement);
        if (mogDataSource == null) {
            if (log.isWarnEnabled())
                log.warn(String.format("Unable to determine datasource for table '%s'", tableElement.getName()));
            return;
        }

        List<? extends MogFetchable> data = (mogDataSource != null)
                ? mogDataSource.fetch(tableElement.getDataSource().getFilter())
                : null;

        List<Column> columns = tableElement.getColumns();
        int rowCount = (data != null && !data.isEmpty()) ? data.size() : 1;
        int columnCount = columns != null ? columns.size() : 0;

        if (log.isDebugEnabled())
            log.debug(String.format("rowCount = %d, columnCount = %d", rowCount, columnCount));

        CellReference topLeft = new CellReference(tableElement.getUpperLeft().getRow(),
                tableElement.getUpperLeft().getCol());
        CellReference bottomRight = new CellReference(tableElement.getUpperLeft().getRow() + rowCount,
                tableElement.getUpperLeft().getCol() + columnCount - 1);
        AreaReference reference = workbook().getCreationHelper().createAreaReference(topLeft, bottomRight);

        // Create
        XSSFTable table = sheet().createTable(reference);
        table.setName(tableElement.getName());
        table.setDisplayName(tableElement.getTitle());

        // For now, create the initial style in a low-level way
        table.getCTTable().addNewTableStyleInfo();
        table.getCTTable().getTableStyleInfo().setName(tableElement.getStyle());

        // Style the table
        // TODO add to report json
        XSSFTableStyleInfo style = (XSSFTableStyleInfo) table.getStyle();
        style.setName(tableElement.getStyle());
        style.setFirstColumn(false);
        style.setLastColumn(false);
        style.setShowRowStripes(true);
        style.setShowColumnStripes(true);

        // reusables
        XSSFRow row;
        XSSFCell cell;
        XSSFTableColumn column;
        int rowNum = tableElement.getUpperLeft().getRow();

        // headers
        if (columns != null) {
            row = sheet().createRow(rowNum++);
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

            colNum = 0;
            Object value;
            if (data != null) {
                for (MogFetchable reportable : data) {
                    row = sheet().createRow(rowNum++);

                    colNum = tableElement.getUpperLeft().getCol();
                    for (Column columnDef : columns) {
                        cell = row.createCell(colNum++);

                        if (columnDef.getKey() != null) {
                            value = reportable.get(columnDef.getKey());

                            if (value != null) {
                                switch (value) {
                                    case Long longValue -> {
                                        cell.setCellValue(longValue);
                                    }
                                    case Integer intValue -> {
                                        cell.setCellValue(intValue);
                                    }
                                    default -> {
                                        cell.setCellValue(value != null ? value.toString() : "");
                                    }
                                }
                            } else {
                                cell.setCellValue("");
                            }
                        }
                    }
                }
            } else {
                row = sheet().createRow(rowNum);
                for (colNum = 0; colNum <= columnCount; colNum++) {
                    cell = row.createCell(colNum);
                    cell.setCellValue("");
                }
            }
        }

        if (tableElement.getEnableFilters())
            table.getCTTable().addNewAutoFilter().setRef(reference.formatAsString());
    }

    /**
     * 
     * @param report
     * @param tableElement
     * @return
     */
    MogDataSource getDataSource(Report report, Table tableElement) {
        ReportDataSource reportDataSource = tableElement.getDataSource();
        if (reportDataSource == null)
            return null;

        if (log.isTraceEnabled())
            log.trace(String.format("reportDataSource = %s", reportDataSource));

        return report.getDataSources().stream()
                .filter(d -> d.getName() != null && d.getName().equals(reportDataSource.getName())).findFirst()
                .orElse(null);
    }

}
