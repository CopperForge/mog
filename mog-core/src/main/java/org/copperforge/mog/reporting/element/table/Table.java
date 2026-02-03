package org.copperforge.mog.reporting.element.table;

import java.util.List;

import org.copperforge.mog.reporting.ReportDataSource;
import org.copperforge.mog.reporting.definition.CellReference;
import org.copperforge.mog.reporting.definition.Column;
import org.copperforge.mog.reporting.element.ReportElement;

import com.fasterxml.jackson.annotation.JsonFormat;

public class Table extends ReportElement {

    private String name;
    private String title;
    private String style;
    private List<Column> columns;
    private CellReference upperLeft;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
    private ReportDataSource dataSource;
    private boolean enableFilters = true;

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public CellReference getUpperLeft() {
        return upperLeft;
    }

    public void setUpperLeft(CellReference upperLeft) {
        this.upperLeft = upperLeft;
    }

    public ReportDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(ReportDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean getEnableFilters() {
        return enableFilters;
    }

    public void setEnableFilters(Boolean enableFilters) {
        this.enableFilters = enableFilters;
    }

    @Override
    public String toString() {
        return "Table [name=" + name + ", title=" + title + ", style=" + style + ", columns=" + columns + ", upperLeft="
                + upperLeft + ", dataSource=" + dataSource + ", enableFilters=" + enableFilters + "]";
    }

}
