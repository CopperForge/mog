package org.copperforge.mog.reporting.xlsx;

import org.apache.poi.ss.usermodel.DataConsolidateFunction;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.element.ReportElement;
import org.copperforge.mog.reporting.element.table.PivotTable;

public class XLSXPivotTableWriter extends XLSXElementWriter<PivotTable> {

    @Override
    public void write(Report report, ReportElement element) throws MogException {
        PivotTable tableElement = (PivotTable) element;

        XSSFTable refTable = workbook().getTable(tableElement.getReferenceTable());
        CellReference position = new CellReference(tableElement.getUpperLeft().getRow() - 1,
                tableElement.getUpperLeft().getCol() - 1);

        // Create a pivot table on this sheet, with H5 as the top-left cell..
        // The pivot table's data source is on the same sheet in A1:D4
        XSSFPivotTable pivotTable = sheet().createPivotTable(refTable, position);

        // Configure the pivot table
        tableElement.getPivotColumns().forEach(c -> pivotTable.addColLabel(c));
        tableElement.getPivotRows().forEach(r -> pivotTable.addRowLabel(r));

        tableElement.getConsolidators().forEach(c -> pivotTable
                .addColumnLabel(DataConsolidateFunction.valueOf(c.getType()), c.getColumn(), c.getValueFieldName()));

    }

}
