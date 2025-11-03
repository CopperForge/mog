package org.copperforge.mog.reporting.xlsx;

import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
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

        validateChartInputs(chartDef);

        int col1 = chartDef.getUpperLeft() != null && chartDef.getUpperLeft().getCol() != null
                ? chartDef.getUpperLeft().getCol() - 1 : 0; // convert 1-based to 0-based
        int row1 = chartDef.getUpperLeft() != null && chartDef.getUpperLeft().getRow() != null
                ? chartDef.getUpperLeft().getRow() - 1 : 0; // convert 1-based to 0-based
        int col2 = col1 + (chartDef.getWidth() != null ? chartDef.getWidth() : 10);
        int row2 = row1 + (chartDef.getHeight() != null ? chartDef.getHeight() : 15);

        XSSFDrawing drawing = sheet().createDrawingPatriarch();
        XSSFClientAnchor anchor = workbook().getCreationHelper().createClientAnchor();
        anchor.setCol1(col1);
        anchor.setRow1(row1);
        anchor.setCol2(col2);
        anchor.setRow2(row2);

        XSSFChart chart = drawing.createChart(anchor);
        if (chartDef.getTitle() != null && !chartDef.getTitle().isBlank()) {
            chart.setTitleText(chartDef.getTitle());
            chart.setTitleOverlay(false);
        }

        if (!chartDef.isShowLegend()) {
            chart.getCTChart().unsetLegend();
        } else {
            XDDFChartLegend legend = chart.getOrAddLegend();
            legend.setPosition(LegendPosition.TOP);
            legend.setOverlay(false);
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

    private void validateChartInputs(Chart chartDef) throws MogException {
        if (chartDef.getUpperLeft() == null || chartDef.getUpperLeft().getRow() == null
                || chartDef.getUpperLeft().getCol() == null) {
            throw new MogException("Chart '" + chartDef.getName() + "' requires upperLeft row and col (1-based)");
        }
        if (chartDef.getUpperLeft().getRow() < 1 || chartDef.getUpperLeft().getCol() < 1) {
            throw new MogException("Chart '" + chartDef.getName() + "' coordinates must be >= 1");
        }
        if (chartDef.getSeries() == null || chartDef.getSeries().isEmpty()) {
            throw new MogException("Chart '" + chartDef.getName() + "' requires at least one series");
        }
        if (chartDef.getCategory() == null && chartDef.getChartType() != null
                && !chartDef.getChartType().equalsIgnoreCase("pie")) {
            throw new MogException("Chart '" + chartDef.getName() + "' requires a category for non-pie charts");
        }
    }

    private void plotBarChart(XSSFChart chart, Chart chartDef) throws MogException {
        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);

        XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        data.setBarDirection(BarDirection.COL);
        if (Boolean.TRUE.equals(chartDef.getStacked())) data.setVaryColors(false);

        XDDFDataSource<?> categories = resolveCategories(sheet(), chartDef);
        for (ChartSeries s : chartDef.getSeries()) {
            XDDFNumericalDataSource<Double> values = resolveSeries(sheet(), chartDef, s);
            XDDFChartData.Series series = data.addSeries(categories, values);
            if (s.getName() != null) series.setTitle(s.getName(), null);
        }
        chart.plot(data);
    }

    private void plotCategoryValueChart(XSSFChart chart, Chart chartDef, ChartTypes type) throws MogException {
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

    private void plotPieChart(XSSFChart chart, Chart chartDef) throws MogException {
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
        AreaReference area = table.getArea();
        if (area == null) throw new MogException("Table has no area defined");
        int columnCount = table.getColumnCount();
        if (columnIndex < 1 || columnIndex > columnCount) {
            throw new MogException("Chart column index out of bounds: " + columnIndex + " (1.." + columnCount + ")");
        }
        CellReference first = area.getFirstCell();
        CellReference last = area.getLastCell();
        int firstRow = first.getRow() + 1; // skip header row
        int lastRow = Math.max(firstRow, last.getRow());
        int firstCol = first.getCol() + (columnIndex - 1);
        int lastCol = firstCol;
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
