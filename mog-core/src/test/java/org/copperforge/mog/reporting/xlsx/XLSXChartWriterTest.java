package org.copperforge.mog.reporting.xlsx;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.element.chart.Chart;
import org.copperforge.mog.reporting.element.chart.ChartCategory;
import org.copperforge.mog.reporting.element.chart.ChartSeries;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumns;

public class XLSXChartWriterTest {

    @Test
    void barChart_requiresCategory() {
        XLSXChartWriter writer = new XLSXChartWriter();
        XSSFWorkbook wb = new XSSFWorkbook();
        writer.workbook(wb).sheet(wb.createSheet("S"));

        Chart chart = baseChart("c1", "bar");
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
        var upperLeft = new org.copperforge.mog.reporting.definition.CellReference();
        upperLeft.setRow(0);
        upperLeft.setCol(0);
        chart.setUpperLeft(upperLeft);
        chart.getSeries().add(new ChartSeries());

        assertThrows(MogException.class, () -> writer.write(new Report(), chart));
    }

    @Test
    void series_column_outOfBounds_throws() {
        XLSXChartWriter writer = new XLSXChartWriter();
        XSSFWorkbook wb = new XSSFWorkbook();
        var sheet = wb.createSheet("S");
        writer.workbook(wb).sheet(sheet);
        createTable(sheet, "t_sales", 2);

        Chart chart = baseChart("c3", "bar");
        chart.setCategory(category("t_sales", 1));

        ChartSeries series = new ChartSeries();
        series.setTable("t_sales");
        series.setColumn(3);
        chart.getSeries().add(series);

        assertThrows(MogException.class, () -> writer.write(new Report(), chart));
    }

    @Test
    void stackedBarChart_isSupported() {
        XLSXChartWriter writer = new XLSXChartWriter();
        XSSFWorkbook wb = new XSSFWorkbook();
        var sheet = wb.createSheet("S");
        writer.workbook(wb).sheet(sheet);
        createTable(sheet, "t_sales", 3);

        Chart chart = baseChart("stacked-bar", "bar");
        chart.setCategory(category("t_sales", 1));
        chart.setStacked(true);

        ChartSeries first = new ChartSeries();
        first.setTable("t_sales");
        first.setColumn(2);
        chart.getSeries().add(first);

        ChartSeries second = new ChartSeries();
        second.setTable("t_sales");
        second.setColumn(3);
        chart.getSeries().add(second);

        assertDoesNotThrow(() -> writer.write(new Report(), chart));
    }

    @Test
    void percentStackedLineChart_isRejected() {
        XLSXChartWriter writer = new XLSXChartWriter();
        XSSFWorkbook wb = new XSSFWorkbook();
        writer.workbook(wb).sheet(wb.createSheet("S"));

        Chart chart = baseChart("percent-line", "line");
        chart.setCategory(category("t_sales", 1));
        chart.setPercentStacked(true);
        chart.getSeries().add(new ChartSeries());

        assertThrows(MogException.class, () -> writer.write(new Report(), chart));
    }

    @Test
    void dataLabels_areRejected() {
        XLSXChartWriter writer = new XLSXChartWriter();
        XSSFWorkbook wb = new XSSFWorkbook();
        writer.workbook(wb).sheet(wb.createSheet("S"));

        Chart chart = baseChart("labels", "bar");
        chart.setCategory(category("t_sales", 1));
        chart.setDataLabels(true);
        chart.getSeries().add(new ChartSeries());

        assertThrows(MogException.class, () -> writer.write(new Report(), chart));
    }

    @Test
    void secondarySeriesAxis_isRejected() {
        XLSXChartWriter writer = new XLSXChartWriter();
        XSSFWorkbook wb = new XSSFWorkbook();
        writer.workbook(wb).sheet(wb.createSheet("S"));

        Chart chart = baseChart("secondary-axis", "bar");
        chart.setCategory(category("t_sales", 1));
        ChartSeries series = new ChartSeries();
        series.setAxis("secondary");
        chart.getSeries().add(series);

        assertThrows(MogException.class, () -> writer.write(new Report(), chart));
    }

    private Chart baseChart(String name, String type) {
        Chart chart = new Chart();
        chart.setName(name);
        chart.setChartType(type);
        var upperLeft = new org.copperforge.mog.reporting.definition.CellReference();
        upperLeft.setRow(1);
        upperLeft.setCol(1);
        chart.setUpperLeft(upperLeft);
        return chart;
    }

    private ChartCategory category(String table, int column) {
        ChartCategory category = new ChartCategory();
        category.setTable(table);
        category.setColumn(column);
        return category;
    }

    private void createTable(XSSFSheet sheet, String tableName, int columnCount) {
        var header = sheet.createRow(0);
        header.createCell(0).setCellValue("Category");
        var data = sheet.createRow(1);
        data.createCell(0).setCellValue("x");
        for (int index = 1; index < columnCount; index++) {
            header.createCell(index).setCellValue("Value " + index);
            data.createCell(index).setCellValue(index);
        }

        AreaReference area = new AreaReference(new CellReference(0, 0), new CellReference(1, columnCount - 1),
                SpreadsheetVersion.EXCEL2007);
        XSSFTable table = sheet.createTable(area);
        table.setName(tableName);
        table.setDisplayName(tableName);
        CTTable ct = table.getCTTable();
        CTTableColumns columns = ct.getTableColumns();
        if (columns == null) {
            columns = ct.addNewTableColumns();
        }
        columns.setCount(columnCount);
        if (columns.sizeOfTableColumnArray() < columnCount) {
            for (int index = columns.sizeOfTableColumnArray(); index < columnCount; index++) {
                var column = columns.addNewTableColumn();
                column.setId(index + 1L);
                column.setName(index == 0 ? "Category" : "Value " + index);
            }
        }
    }
}
