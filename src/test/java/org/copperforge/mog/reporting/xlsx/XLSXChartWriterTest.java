package org.copperforge.mog.reporting.xlsx;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.element.chart.Chart;
import org.copperforge.mog.reporting.element.chart.ChartSeries;
import org.junit.jupiter.api.Test;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumns;

public class XLSXChartWriterTest {

    @Test
    void barChart_requiresCategory() {
        XLSXChartWriter writer = new XLSXChartWriter();
        XSSFWorkbook wb = new XSSFWorkbook();
        writer.workbook(wb).sheet(wb.createSheet("S"));

        Chart chart = new Chart();
        chart.setName("c1");
        chart.setChartType("bar");
        var ul = new org.copperforge.mog.reporting.definition.CellReference();
        ul.setRow(1); ul.setCol(1);
        chart.setUpperLeft(ul);
        chart.getSeries().add(new ChartSeries());

        assertThrows(MogException.class, () -> writer.write(new Report(), chart));
    }

    @Test
    void chart_upperLeft_mustBeOneBased() {
        XLSXChartWriter writer = new XLSXChartWriter();
        XSSFWorkbook wb = new XSSFWorkbook();
        writer.workbook(wb).sheet(wb.createSheet("S"));

        Chart chart = new Chart();
        chart.setName("c2");
        chart.setChartType("line");
        var ul = new org.copperforge.mog.reporting.definition.CellReference();
        ul.setRow(0); ul.setCol(0);
        chart.setUpperLeft(ul);
        chart.getSeries().add(new ChartSeries());

        assertThrows(MogException.class, () -> writer.write(new Report(), chart));
    }

    @Test
    void series_column_outOfBounds_throws() {
        XLSXChartWriter writer = new XLSXChartWriter();
        XSSFWorkbook wb = new XSSFWorkbook();
        var sheet = wb.createSheet("S");
        writer.workbook(wb).sheet(sheet);

        // Create a minimal 2-column table A1:B2
        var header = sheet.createRow(0);
        header.createCell(0).setCellValue("A");
        header.createCell(1).setCellValue("B");
        var data = sheet.createRow(1);
        data.createCell(0).setCellValue("x");
        data.createCell(1).setCellValue(1);

        AreaReference area = new AreaReference(new CellReference(0, 0), new CellReference(1, 1), SpreadsheetVersion.EXCEL2007);
        XSSFTable table = sheet.createTable(area);
        table.setName("t_sales");
        table.setDisplayName("t_sales");
        // Ensure column count is recognized as 2
        CTTable ct = table.getCTTable();
        CTTableColumns cols = ct.getTableColumns();
        if (cols == null) cols = ct.addNewTableColumns();
        cols.setCount(2);
        if (cols.sizeOfTableColumnArray() < 2) {
            var c1 = cols.addNewTableColumn(); c1.setId(1); c1.setName("A");
            var c2 = cols.addNewTableColumn(); c2.setId(2); c2.setName("B");
        }

        Chart chart = new Chart();
        chart.setName("c3");
        chart.setChartType("bar");
        var ul = new org.copperforge.mog.reporting.definition.CellReference();
        ul.setRow(1); ul.setCol(1);
        chart.setUpperLeft(ul);

        var cat = new org.copperforge.mog.reporting.element.chart.ChartCategory();
        cat.setTable("t_sales");
        cat.setColumn(1); // valid
        chart.setCategory(cat);

        var s = new ChartSeries();
        s.setTable("t_sales");
        s.setColumn(3); // out of bounds (only 2 columns)
        chart.getSeries().add(s);

        assertThrows(MogException.class, () -> writer.write(new Report(), chart));
    }
}
