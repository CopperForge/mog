package org.copperforge.mog.reporting.xlsx;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.element.chart.Chart;
import org.copperforge.mog.reporting.element.chart.ChartSeries;
import org.junit.jupiter.api.Test;

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
}

