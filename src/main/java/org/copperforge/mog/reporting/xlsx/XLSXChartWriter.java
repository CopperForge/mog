package org.copperforge.mog.reporting.xlsx;

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.XDDFChart;
import org.apache.poi.xddf.usermodel.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.XDDFLegendPosition;
import org.apache.poi.xddf.usermodel.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.XDDFValueAxis;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFBarDirection;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.element.ReportElement;
import org.copperforge.mog.reporting.element.chart.Chart;
import org.copperforge.mog.reporting.element.chart.ChartSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;

public class XLSXChartWriter extends XLSXElementWriter<Chart> {

    private static final Logger log = LoggerFactory.getLogger(XLSXChartWriter.class);

    @Override
    public void write(Report report, ReportElement element) throws MogException {
        Chart chartDef = (Chart) element;

        int col1 = chartDef.getUpperLeft() != null && chartDef.getUpperLeft().getCol() != null
                ? chartDef.getUpperLeft().getCol() : 1;
        int row1 = chartDef.getUpperLeft() != null && chartDef.getUpperLeft().getRow() != null
                ? chartDef.getUpperLeft().getRow() : 1;
        int col2 = col1 + (chartDef.getWidth() != null ? chartDef.getWidth() : 10);
        int row2 = row1 + (chartDef.getHeight() != null ? chartDef.getHeight() : 15);

        XSSFDrawing drawing = sheet().createDrawingPatriarch();
        XSSFClientAnchor anchor = workbook().getCreationHelper().createClientAnchor();
        anchor.setCol1(col1);
        anchor.setRow1(row1);
        anchor.setCol2(col2);
        anchor.setRow2(row2);

        XDDFChart chart = drawing.createChart(anchor);
        if (chartDef.getTitle() != null && !chartDef.getTitle().isBlank()) {
            chart.setTitleText(chartDef.getTitle());
            chart.setTitleOverlay(false);
        }

        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.TOP);
        legend.setOverlay(false);
        if (!chartDef.isShowLegend()) {
            legend.setPosition(LegendPosition.NONE);
        }

        String type = chartDef.getChartType() != null ? chartDef.getChartType().toLowerCase() : "bar";

        switch (type) {
            case "line" -> plotCategoryValueChart(chart, chartDef, ChartTypes.LINE);
            case "area" -> plotCategoryValueChart(chart, chartDef, ChartTypes.AREA);
            case "pie" -> plotPieChart(chart, chartDef);
            case "scatter" -> plotCategoryValueChart(chart, chartDef, ChartTypes.SCATTER);
            default -> plotBarChart(chart, chartDef);
        }
    }

    private void plotBarChart(XDDFChart chart, Chart chartDef) throws MogException {
        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);

        XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        data.setBarDirection(XDDFBarDirection.COL);
        if (Boolean.TRUE.equals(chartDef.getStacked())) data.setVaryColors(false);

        XDDFDataSource<?> categories = resolveCategories(sheet(), chartDef);
        for (ChartSeries s : chartDef.getSeries()) {
            XDDFNumericalDataSource<Double> values = resolveSeries(sheet(), chartDef, s);
            XDDFChartData.Series series = data.addSeries(categories, values);
            if (s.getName() != null) series.setTitle(s.getName(), null);
        }
        chart.plot(data);
    }

    private void plotCategoryValueChart(XDDFChart chart, Chart chartDef, ChartTypes type) throws MogException {
        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        XDDFChartData data = chart.createData(type, bottomAxis, leftAxis);
        XDDFDataSource<?> categories = resolveCategories(sheet(), chartDef);
        for (ChartSeries s : chartDef.getSeries()) {
            XDDFNumericalDataSource<Double> values = resolveSeries(sheet(), chartDef, s);
            XDDFChartData.Series series = data.addSeries(categories, values);
            if (s.getName() != null) series.setTitle(s.getName(), null);
        }
        chart.plot(data);
    }

    private void plotPieChart(XDDFChart chart, Chart chartDef) throws MogException {
        XDDFChartData data = chart.createData(ChartTypes.PIE, null, null);
        XDDFDataSource<?> categories = resolveCategories(sheet(), chartDef);
        for (ChartSeries s : chartDef.getSeries()) {
            XDDFNumericalDataSource<Double> values = resolveSeries(sheet(), chartDef, s);
            XDDFChartData.Series series = data.addSeries(categories, values);
            if (s.getName() != null) series.setTitle(s.getName(), null);
        }
        chart.plot(data);
    }

    private XDDFDataSource<?> resolveCategories(XSSFSheet sheet, Chart chartDef) throws MogException {
        // support table+column; explicit range may be added later
        if (chartDef.getCategory() == null) throw new MogException("Chart category is required");
        if (chartDef.getCategory().getRange() != null) {
            CellRangeAddress addr = toCellRange(chartDef.getCategory().getRange());
            return XDDFDataSourcesFactory.fromStringCellRange(sheet, addr);
        }
        String tableName = chartDef.getCategory().getTable();
        Integer column = chartDef.getCategory().getColumn();
        XSSFTable table = findTable(sheet, tableName);
        CellRangeAddress addr = columnRangeForTable(table, column);
        return XDDFDataSourcesFactory.fromStringCellRange(sheet, addr);
    }

    private XDDFNumericalDataSource<Double> resolveSeries(XSSFSheet sheet, Chart chartDef, ChartSeries s)
            throws MogException {
        if (s.getRange() != null) {
            CellRangeAddress addr = toCellRange(s.getRange());
            return XDDFDataSourcesFactory.fromNumericCellRange(sheet, addr);
        }
        String tableName = s.getTable() != null ? s.getTable()
                : (chartDef.getCategory() != null ? chartDef.getCategory().getTable() : null);
        Integer column = s.getColumn();
        XSSFTable table = findTable(sheet, tableName);
        CellRangeAddress addr = columnRangeForTable(table, column);
        return XDDFDataSourcesFactory.fromNumericCellRange(sheet, addr);
    }

    private XSSFTable findTable(XSSFSheet sheet, String name) throws MogException {
        if (name == null) throw new MogException("Chart requires a table name or explicit range");
        for (XSSFTable t : sheet.getTables()) {
            if (name.equals(t.getName())) return t;
        }
        throw new MogException("Table not found for chart: " + name);
    }

    private CellRangeAddress columnRangeForTable(XSSFTable table, int columnIndex) throws MogException {
        // Use table bounds, skip header row
        int firstRow = table.getStartRow() + 1;
        int lastRow = table.getEndRow();
        int firstCol = table.getStartCol() + columnIndex;
        int lastCol = firstCol;
        if (firstRow > lastRow) {
            // create a single empty cell range under the header to keep chart valid
            firstRow = table.getStartRow() + 1;
            lastRow = firstRow;
        }
        return new CellRangeAddress(firstRow, lastRow, firstCol, lastCol);
    }

    private CellRangeAddress toCellRange(String a1) throws MogException {
        try {
            String[] parts = a1.split("!");
            String range = parts.length == 2 ? parts[1] : parts[0];
            return CellRangeAddress.valueOf(range);
        } catch (Exception e) {
            throw new MogException("Invalid cell range: " + a1, e);
        }
    }
}

