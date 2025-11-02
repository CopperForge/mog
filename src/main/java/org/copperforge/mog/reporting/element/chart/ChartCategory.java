package org.copperforge.mog.reporting.element.chart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChartCategory {
    private String table;   // backing table for categories (optional if range provided)
    private Integer column; // column index (0-based)
    private String range;   // explicit range, e.g., Sheet1!A2:A100

    public String getTable() { return table; }
    public void setTable(String table) { this.table = table; }

    public Integer getColumn() { return column; }
    public void setColumn(Integer column) { this.column = column; }

    public String getRange() { return range; }
    public void setRange(String range) { this.range = range; }

    @Override
    public String toString() {
        return "ChartCategory [table=" + table + ", column=" + column + ", range=" + range + "]";
    }
}

