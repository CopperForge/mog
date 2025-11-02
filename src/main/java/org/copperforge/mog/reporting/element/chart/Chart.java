package org.copperforge.mog.reporting.element.chart;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.copperforge.mog.reporting.definition.CellReference;
import org.copperforge.mog.reporting.element.ReportElement;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Chart extends ReportElement {

    private String name;
    private String chartType; // bar, line, area, pie, scatter
    private String title;
    private boolean showLegend = true;

    private CellReference upperLeft; // anchor cell for the chart
    private Integer width;  // width in columns (approximate)
    private Integer height; // height in rows (approximate), overrides base height if provided

    private ChartCategory category; // x-axis categories
    private final List<ChartSeries> series = new ArrayList<>();

    // Options
    private Boolean stacked;        // for bar/area
    private Boolean percentStacked; // 100% stacked
    private Boolean dataLabels;     // show values
    private Boolean secondaryAxis;  // allow series to use secondary axis when supported

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getChartType() { return chartType; }
    public void setChartType(String chartType) { this.chartType = chartType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isShowLegend() { return showLegend; }
    public void setShowLegend(boolean showLegend) { this.showLegend = showLegend; }

    public CellReference getUpperLeft() { return upperLeft; }
    public void setUpperLeft(CellReference upperLeft) { this.upperLeft = upperLeft; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    @Override
    public Integer getHeight() { return height != null ? height : super.getHeight(); }
    public void setHeight(Integer height) { this.height = height; }

    public ChartCategory getCategory() { return category; }
    public void setCategory(ChartCategory category) { this.category = category; }

    public List<ChartSeries> getSeries() { return series; }
    public void setSeries(List<ChartSeries> s) { this.series.clear(); if (s != null) this.series.addAll(s); }

    public Boolean getStacked() { return stacked; }
    public void setStacked(Boolean stacked) { this.stacked = stacked; }

    public Boolean getPercentStacked() { return percentStacked; }
    public void setPercentStacked(Boolean percentStacked) { this.percentStacked = percentStacked; }

    public Boolean getDataLabels() { return dataLabels; }
    public void setDataLabels(Boolean dataLabels) { this.dataLabels = dataLabels; }

    public Boolean getSecondaryAxis() { return secondaryAxis; }
    public void setSecondaryAxis(Boolean secondaryAxis) { this.secondaryAxis = secondaryAxis; }

    @Override
    public String toString() {
        return "Chart [name=" + name + ", chartType=" + chartType + ", title=" + title + ", showLegend=" + showLegend
                + ", upperLeft=" + upperLeft + ", width=" + width + ", height=" + height
                + ", category=" + category + ", series=" + series + ", stacked=" + stacked
                + ", percentStacked=" + percentStacked + ", dataLabels=" + dataLabels
                + ", secondaryAxis=" + secondaryAxis + "]";
    }
}

