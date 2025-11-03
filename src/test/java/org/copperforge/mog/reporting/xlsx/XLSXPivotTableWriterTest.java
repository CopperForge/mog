package org.copperforge.mog.reporting.xlsx;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.element.table.PivotTable;
import org.junit.jupiter.api.Test;

public class XLSXPivotTableWriterTest {

    @Test
    void pivot_requiresExistingReferenceTable() {
        XLSXPivotTableWriter writer = new XLSXPivotTableWriter();
        XSSFWorkbook wb = new XSSFWorkbook();
        writer.workbook(wb).sheet(wb.createSheet("S"));

        PivotTable pt = new PivotTable();
        var ul = new org.copperforge.mog.reporting.definition.CellReference();
        ul.setRow(1); ul.setCol(1);
        pt.setUpperLeft(ul);
        pt.setReferenceTable("missing_table");

        assertThrows(MogException.class, () -> writer.write(new Report(), pt));
    }
}

