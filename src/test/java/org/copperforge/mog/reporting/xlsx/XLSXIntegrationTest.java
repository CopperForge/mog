package org.copperforge.mog.reporting.xlsx;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.definition.Column;
import org.copperforge.mog.reporting.element.chart.Chart;
import org.copperforge.mog.reporting.element.chart.ChartCategory;
import org.copperforge.mog.reporting.element.chart.ChartSeries;
import org.copperforge.mog.reporting.element.table.Table;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.copperforge.mog.reporting.element.table.PivotTable;
import org.copperforge.mog.reporting.xlsx.XLSXPivotTableConsolidator;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPivotTableDefinition;

public class XLSXIntegrationTest {

    @Test
    void chartFromTable_happyPath_createsOneSeries() throws MogException {
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("S");

        // Build a 3-column table A1:C5 (header + 4 rows)
        XSSFRow header = sheet.createRow(0);
        header.createCell(0).setCellValue("Region");
        header.createCell(1).setCellValue("Revenue");
        header.createCell(2).setCellValue("Units");
        String[] regions = {"North","South","West","East"};
        int[] revenue = {100,120,90,140};
        int[] units = {20,30,25,35};
        for (int i = 0; i < regions.length; i++) {
            XSSFRow r = sheet.createRow(i+1);
            r.createCell(0).setCellValue(regions[i]);
            r.createCell(1).setCellValue(revenue[i]);
            r.createCell(2).setCellValue(units[i]);
        }

        AreaReference area = new AreaReference(new CellReference(0,0), new CellReference(4,2), SpreadsheetVersion.EXCEL2007);
        XSSFTable table = sheet.createTable(area);
        table.setName("t_sales");
        table.setDisplayName("t_sales");
        table.getCTTable().addNewTableColumns().setCount(3);
        table.getCTTable().getTableColumns().addNewTableColumn().setId(1);
        table.getCTTable().getTableColumns().getTableColumnArray(0).setName("Region");
        table.getCTTable().getTableColumns().addNewTableColumn().setId(2);
        table.getCTTable().getTableColumns().getTableColumnArray(1).setName("Revenue");
        table.getCTTable().getTableColumns().addNewTableColumn().setId(3);
        table.getCTTable().getTableColumns().getTableColumnArray(2).setName("Units");

        // Define a chart bound to the table: category=col1, series=col2
        Chart chartDef = new Chart();
        chartDef.setName("c_sales");
        chartDef.setChartType("bar");
        var ul = new org.copperforge.mog.reporting.definition.CellReference(); ul.setRow(1); ul.setCol(5);
        chartDef.setUpperLeft(ul);
        ChartCategory cat = new ChartCategory();
        cat.setTable("t_sales");
        cat.setColumn(1);
        chartDef.setCategory(cat);
        ChartSeries series = new ChartSeries();
        series.setTable("t_sales");
        series.setColumn(2);
        List<ChartSeries> list = new ArrayList<>(); list.add(series);
        chartDef.setSeries(list);

        XLSXChartWriter chartWriter = new XLSXChartWriter();
        chartWriter.workbook(wb).sheet(sheet).write(new org.copperforge.mog.reporting.definition.Report(), chartDef);

        // Inspect the chart that was created
        var drawing = sheet.getDrawingPatriarch();
        assertNotNull(drawing);
        List<XSSFChart> charts = drawing.getCharts();
        assertEquals(1, charts.size());
        XSSFChart chart = charts.get(0);
        CTPlotArea plot = chart.getCTChart().getPlotArea();
        CTBarChart bar = plot.getBarChartArray(0);
        assertEquals(1, bar.getSerList().size());
        // Legend hidden check if configured via showLegend=false could be added here
    }

    @Test
    void tableDisplayName_isSanitized() throws MogException {
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("S");

        Table t = new Table();
        t.setName("t sales");
        t.setTitle("Sales 2025!");
        var ul = new org.copperforge.mog.reporting.definition.CellReference(); ul.setRow(1); ul.setCol(1);
        t.setUpperLeft(ul);
        List<Column> cols = new ArrayList<>();
        var c1 = new Column(); c1.setTitle("Region"); cols.add(c1);
        var c2 = new Column(); c2.setTitle("Revenue"); cols.add(c2);
        t.setColumns(cols);

        XLSXTableWriter writer = new XLSXTableWriter();
        writer.workbook(wb).sheet(sheet).write(new XLSXReport(), t);

        XSSFTable table = sheet.getTables().get(0);
        String display = table.getCTTable().getDisplayName();
        assertEquals("Sales_2025_", display);
    }

    @Test
    void chartLegend_hidden_unsetsLegend() throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("S");

        // Use the same 3-column, 4-row setup as the happy path (more robust for chart enumeration)
        XSSFRow header = sheet.createRow(0);
        header.createCell(0).setCellValue("Region");
        header.createCell(1).setCellValue("Revenue");
        header.createCell(2).setCellValue("Units");
        String[] regions = {"North","South","West","East"};
        int[] revenue = {100,120,90,140};
        int[] units = {20,30,25,35};
        for (int i = 0; i < regions.length; i++) {
            XSSFRow r = sheet.createRow(i+1);
            r.createCell(0).setCellValue(regions[i]);
            r.createCell(1).setCellValue(revenue[i]);
            r.createCell(2).setCellValue(units[i]);
        }
        AreaReference area = new AreaReference(new CellReference(0,0), new CellReference(4,2), SpreadsheetVersion.EXCEL2007);
        XSSFTable table = sheet.createTable(area);
        table.setName("t_sales");
        table.setDisplayName("t_sales");
        table.getCTTable().addNewTableColumns().setCount(3);
        table.getCTTable().getTableColumns().addNewTableColumn().setId(1);
        table.getCTTable().getTableColumns().getTableColumnArray(0).setName("Region");
        table.getCTTable().getTableColumns().addNewTableColumn().setId(2);
        table.getCTTable().getTableColumns().getTableColumnArray(1).setName("Revenue");
        table.getCTTable().getTableColumns().addNewTableColumn().setId(3);
        table.getCTTable().getTableColumns().getTableColumnArray(2).setName("Units");

        Chart chartDef = new Chart();
        chartDef.setName("c_legend");
        chartDef.setChartType("bar");
        chartDef.setShowLegend(false);
        var ul = new org.copperforge.mog.reporting.definition.CellReference(); ul.setRow(1); ul.setCol(8);
        chartDef.setUpperLeft(ul);
        ChartCategory cat = new ChartCategory();
        cat.setTable("t_sales");
        cat.setColumn(1);
        chartDef.setCategory(cat);
        ChartSeries series = new ChartSeries();
        series.setTable("t_sales");
        series.setColumn(2);
        List<ChartSeries> sList = new ArrayList<>(); sList.add(series);
        chartDef.setSeries(sList);

        XLSXChartWriter writer = new XLSXChartWriter();
        writer.workbook(wb).sheet(sheet).write(new org.copperforge.mog.reporting.definition.Report(), chartDef);

        // Round-trip the workbook to force POI to realize drawing relationships, then re-open
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        wb.write(baos);
        wb.close();
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());
        XSSFWorkbook wb2 = new XSSFWorkbook(bais);
        XSSFSheet sheet2 = wb2.getSheet("S");
        var drawing2 = sheet2.getDrawingPatriarch();
        assertNotNull(drawing2);
        // Some environments return an empty list from getCharts(); fallback to scanning relations
        java.util.List<org.apache.poi.ooxml.POIXMLDocumentPart> parts = drawing2.getRelations();
        org.apache.poi.xssf.usermodel.XSSFChart found = null;
        for (org.apache.poi.ooxml.POIXMLDocumentPart p : parts) {
            if (p instanceof org.apache.poi.xssf.usermodel.XSSFChart c) {
                found = c; break;
            }
        }
        assertNotNull(found);
        assertFalse(found.getCTChart().isSetLegend());
        wb2.close();
    }

    @Test
    void tableAutoFilter_refMatchesTableRef_whenEnabled() throws MogException {
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("S");

        Table t = new Table();
        t.setName("t_sales");
        t.setTitle("Sales");
        var ul = new org.copperforge.mog.reporting.definition.CellReference(); ul.setRow(1); ul.setCol(1);
        t.setUpperLeft(ul);
        List<Column> cols = new ArrayList<>();
        var c1 = new Column(); c1.setTitle("Region"); cols.add(c1);
        var c2 = new Column(); c2.setTitle("Revenue"); cols.add(c2);
        t.setColumns(cols);
        t.setEnableFilters(true);

        XLSXTableWriter writer = new XLSXTableWriter();
        writer.workbook(wb).sheet(sheet).write(new XLSXReport(), t);

        // There will be 1 header + 1 data row (empty placeholder) and 2 columns
        var expected = writer.getAreaReference(t, cols.size(), 1).formatAsString();

        XSSFTable table = sheet.getTables().get(0);
        var ct = table.getCTTable();
        assertNotNull(ct.getAutoFilter());
        assertEquals(expected, ct.getAutoFilter().getRef());
    }

    @Test
    void tableAutoFilter_absent_whenDisabled() throws MogException {
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("S");

        Table t = new Table();
        t.setName("t_sales");
        t.setTitle("Sales");
        var ul = new org.copperforge.mog.reporting.definition.CellReference(); ul.setRow(1); ul.setCol(1);
        t.setUpperLeft(ul);
        List<Column> cols = new ArrayList<>();
        var c1 = new Column(); c1.setTitle("Region"); cols.add(c1);
        var c2 = new Column(); c2.setTitle("Revenue"); cols.add(c2);
        t.setColumns(cols);
        t.setEnableFilters(false);

        XLSXTableWriter writer = new XLSXTableWriter();
        writer.workbook(wb).sheet(sheet).write(new XLSXReport(), t);

        XSSFTable table = sheet.getTables().get(0);
        var ct = table.getCTTable();
        assertNull(ct.getAutoFilter());
    }

    @Test
    void pivotTable_happyPath_rowsColsAndData() throws MogException {
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("S");

        // Build a small table Region/Revenue
        XSSFRow header = sheet.createRow(0);
        header.createCell(0).setCellValue("Region");
        header.createCell(1).setCellValue("Revenue");
        String[] regions = {"North","South","North","South"};
        int[] revenue = {10, 20, 30, 40};
        for (int i = 0; i < regions.length; i++) {
            XSSFRow r = sheet.createRow(i+1);
            r.createCell(0).setCellValue(regions[i]);
            r.createCell(1).setCellValue(revenue[i]);
        }
        AreaReference area = new AreaReference(new CellReference(0,0), new CellReference(4,1), SpreadsheetVersion.EXCEL2007);
        XSSFTable table = sheet.createTable(area);
        table.setName("t_sales");
        table.setDisplayName("t_sales");
        table.getCTTable().addNewTableColumns().setCount(2);
        table.getCTTable().getTableColumns().addNewTableColumn().setId(1);
        table.getCTTable().getTableColumns().getTableColumnArray(0).setName("Region");
        table.getCTTable().getTableColumns().addNewTableColumn().setId(2);
        table.getCTTable().getTableColumns().getTableColumnArray(1).setName("Revenue");

        // Define pivot: Row label on Region (col 0), data SUM of Revenue (col 1)
        PivotTable ptDef = new PivotTable();
        var ul = new org.copperforge.mog.reporting.definition.CellReference(); ul.setRow(6); ul.setCol(1);
        ptDef.setUpperLeft(ul);
        ptDef.setReferenceTable("t_sales");
        ptDef.getPivotRows().add(0); // 0-based column index
        XLSXPivotTableConsolidator cons = new XLSXPivotTableConsolidator();
        cons.setType("SUM");
        cons.setColumn(1); // revenue column
        cons.setValueFieldName("Sum of Revenue");
        List<XLSXPivotTableConsolidator> consList = new ArrayList<>(); consList.add(cons);
        ptDef.setConsolidators(consList);

        XLSXPivotTableWriter ptWriter = new XLSXPivotTableWriter();
        ptWriter.workbook(wb).sheet(sheet).write(new org.copperforge.mog.reporting.definition.Report(), ptDef);

        // Assert one pivot exists and has expected fields
        List<XSSFPivotTable> pivots = sheet.getPivotTables();
        assertEquals(1, pivots.size());
        XSSFPivotTable pivot = pivots.get(0);
        CTPivotTableDefinition def = pivot.getCTPivotTableDefinition();
        assertTrue(def.isSetRowFields());
        assertEquals(1L, def.getRowFields().getCount());
        assertTrue(def.isSetDataFields());
        assertEquals(1L, def.getDataFields().getCount());
    }
}
