package org.copperforge.mog.reporting.xlsx;

import java.util.List;

import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFTableColumn;
import org.apache.poi.xssf.usermodel.XSSFTableStyleInfo;

import org.copperforge.mog.reporting.core.ReportException;
import org.copperforge.mog.reporting.core.Reportable;
import org.copperforge.mog.reporting.definition.Column;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.element.ReportElement;
import org.copperforge.mog.reporting.element.table.Table;

public class XLSXTableWriter extends XLSXElementWriter<Table> {

    @Override
    public void write(Report report, ReportElement element) throws ReportException {
        Table tableElement = (Table) element;

        // get the data
        List<Reportable> data = (tableElement.getDataSource() != null) ? tableElement.getDataSource().data() : null;
        List<Column> columns = tableElement.getColumns();
        int rowCount = (data != null) ? data.size() : 1;
        int columnCount = columns != null ? columns.size() : 0;

        AreaReference reference = workbook().getCreationHelper()
                .createAreaReference(new CellReference(tableElement.getUpperLeft().getRow(),
                        tableElement.getUpperLeft().getCol()),
                        new CellReference(tableElement.getUpperLeft().getRow() + rowCount,
                                tableElement.getUpperLeft().getCol() +
                                        columnCount - 1));

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

                column = table.getColumns().get(colNum);
                column.setName(columnDef.getTitle());
                cell = row.createCell(colNum++);
                cell.setCellValue(columnDef.getTitle());
            }

            colNum = 0;
            Object value;
            if (data != null) {
                for (Reportable reportable : data) {
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
                            cell.setCellValue(value.toString());                        
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

        if (tableElement.getEnableFilters()) table.getCTTable().addNewAutoFilter().setRef(reference.formatAsString());
    }

}
