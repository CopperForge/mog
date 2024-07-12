package org.copperforge.mog.reporting.xlsx;

public class XLSXPivotTableConsolidator {

    private String type;

    private Integer column;

    private String valueFieldName;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getColumn() {
        return column;
    }

    public void setColumn(Integer column) {
        this.column = column;
    }

    public String getValueFieldName() {
        return valueFieldName;
    }

    public void setValueFieldName(String valueFieldName) {
        this.valueFieldName = valueFieldName;
    }

    @Override
    public String toString() {
        return "XLSXPivotTableConsolidator [type=" + type + ", column=" + column + ", valueFieldName=" + valueFieldName
                + "]";
    }
    
}
