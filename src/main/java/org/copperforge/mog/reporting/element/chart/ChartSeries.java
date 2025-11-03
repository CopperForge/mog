package org.copperforge.mog.reporting.element.chart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChartSeries {
    private String name;     // legend label
    private String table;    // backing table name (optional if range provided)
    private Integer column;  // column index in table (1-based)
    private String range;    // explicit range, e.g., Sheet1!B2:B100
    private String axis;     // "primary" or "secondary"

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTable() { return table; }
    public void setTable(String table) { this.table = table; }

    public Integer getColumn() { return column; }
    public void setColumn(Integer column) { this.column = column; }

    public String getRange() { return range; }
    public void setRange(String range) { this.range = range; }

    public String getAxis() { return axis; }
    public void setAxis(String axis) { this.axis = axis; }

    @Override
    public String toString() {
        return "ChartSeries [name=" + name + ", table=" + table + ", column=" + column + ", range=" + range
                + ", axis=" + axis + "]";
    }
}
