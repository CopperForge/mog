package org.copperforge.mog.reporting.xlsx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.copperforge.mog.MogException;
import org.copperforge.mog.data.MogFetchable;
import org.copperforge.mog.reporting.definition.Column;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.element.table.Table;
import org.junit.jupiter.api.Test;

public class XLSXTableWriterTest {

    @Test
    void getAreaReference_usesOneBasedCoordinates() throws MogException {
        XLSXTableWriter writer = new XLSXTableWriter();
        writer.workbook(new XSSFWorkbook());

        Table table = new Table();
        org.copperforge.mog.reporting.definition.CellReference ul = new org.copperforge.mog.reporting.definition.CellReference();
        ul.setRow(1); // A1
        ul.setCol(1);
        table.setUpperLeft(ul);

        List<Column> cols = new ArrayList<>();
        cols.add(new Column());
        cols.add(new Column());
        cols.add(new Column());
        table.setColumns(cols);

        int rowCount = 4; // data rows
        AreaReference ref = writer.getAreaReference(table, cols.size(), rowCount);

        CellReference first = ref.getFirstCell();
        CellReference last = ref.getLastCell();

        // top-left A1 -> (0,0)
        assertEquals(0, first.getRow());
        assertEquals(0, first.getCol());

        // bottom-right should be row 4 (header + 4 data rows => index 4), col 2 (3 columns => index 2)
        assertEquals(4, last.getRow());
        assertEquals(2, last.getCol());
    }

    @Test
    void writeColumn_writesAtZeroBasedIndex() throws MogException {
        XLSXTableWriter writer = new XLSXTableWriter();
        XSSFWorkbook wb = new XSSFWorkbook();
        writer.workbook(wb).sheet(wb.createSheet("S"));

        XSSFRow row = writer.sheet().createRow(0);

        Column col = new Column();
        col.setTitle("Revenue");
        col.setKey("revenue");

        Map<String, Object> data = new HashMap<>();
        data.put("revenue", Integer.valueOf(123));
        MogFetchable fetch = new MogFetchable(data);

        // pass 1-based column number; writer should create at index 0
        var cell = writer.writeColumn(row, 1, col, fetch);
        assertEquals(0, cell.getColumnIndex());
        assertEquals(123.0, cell.getNumericCellValue());
    }
}

