package org.copperforge.mog.reporting.definition;

public class CellReference {

    private Integer row;

    private Integer col;

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public Integer getCol() {
        return col;
    }

    public void setCol(Integer col) {
        this.col = col;
    }

    @Override
    public String toString() {
        return "ReportCellReferenceDefinition [row=" + row + ", col=" + col + "]";
    }

    
}
