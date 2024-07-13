package org.copperforge.mog.reporting.element.table;

import java.util.List;

import org.copperforge.mog.data.MogDataSource;
import org.copperforge.mog.reporting.definition.CellReference;
import org.copperforge.mog.reporting.definition.Column;
import org.copperforge.mog.reporting.element.ReportElement;

public class Table extends ReportElement {

    private String name;
    private String title;
    private String style;
    private List<Column> columns;
    private CellReference upperLeft;
    private MogDataSource dataSource;
    private Boolean enableFilters = true;

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

    public MogDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(MogDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Boolean getEnableFilters() {
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
