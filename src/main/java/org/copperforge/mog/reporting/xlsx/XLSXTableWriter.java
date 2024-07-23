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
import org.copperforge.mog.data.MogFetchable;
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
        log.debug("Writing element " + element);
        Table tableElement = (Table) element;

        // get the data
        List<MogFetchable> data = (tableElement.getDataSource() != null) ? tableElement.getDataSource().fetch() : null;
        List<Column> columns = tableElement.getColumns();
        int rowCount = (data != null && data.size() > 0) ? data.size() : 1;
        int columnCount = columns != null ? columns.size() : 0;

        log.debug("rowCount = " + rowCount + ", columnCount = " + columnCount);
        
        CellReference topLeft = new CellReference(tableElement.getUpperLeft().getRow(), tableElement.getUpperLeft().getCol());
        CellReference bottomRight = new CellReference(tableElement.getUpperLeft().getRow() + rowCount, 
                                                        tableElement.getUpperLeft().getCol() + columnCount - 1);
        AreaReference reference = workbook().getCreationHelper().createAreaReference(topLeft,bottomRight);

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

                log.debug("table.getColumns() = " + table.getColumnCount() + "; colNum = " + colNum);
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
                        value = reportable.get(columnDef.getKey());

                        if (value instanceof Long) {
                            cell.setCellValue((Long) value);
                        } else if (value instanceof Integer) {
                            cell.setCellValue((Integer) value);
                        } else {
                            cell.setCellValue(value != null ? value.toString() : "");
                        }
                    }
                }
            } else {
                row = sheet().createRow(rowNum++);
                for (colNum = 0; colNum <= columnCount; colNum++) {
                    cell = row.createCell(colNum);
                    cell.setCellValue("");
                }
            }
        }

        if (tableElement.getEnableFilters())
            table.getCTTable().addNewAutoFilter().setRef(reference.formatAsString());
    }

}
