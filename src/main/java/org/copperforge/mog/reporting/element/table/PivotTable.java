package org.copperforge.mog.reporting.element.table;

import java.util.ArrayList;
import java.util.List;

import org.copperforge.mog.reporting.definition.CellReference;
import org.copperforge.mog.reporting.element.ReportElement;
import org.copperforge.mog.reporting.xlsx.XLSXPivotTableConsolidator;

public class PivotTable extends ReportElement {

    private String name;
    private final List<Integer> pivotColumns = new ArrayList<>();
    private final List<Integer> pivotRows = new ArrayList<>();
    private final List<XLSXPivotTableConsolidator> consolidators = new ArrayList<>();
    private CellReference upperLeft;
    private String referenceTable;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CellReference getUpperLeft() {
        return upperLeft;
    }

    public void setUpperLeft(CellReference upperLeft) {
        this.upperLeft = upperLeft;
    }

    public String getReferenceTable() {
        return referenceTable;
    }

    public void setReferenceTable(String referenceTable) {
        this.referenceTable = referenceTable;
    }

    public List<Integer> getPivotColumns() {
        return pivotColumns;
    }

    public void setPivotColumns(List<Integer> pivotColumns) {
        this.pivotColumns.clear();
        this.pivotColumns.addAll(pivotColumns);
    }

    public List<Integer> getPivotRows() {
        return pivotRows;
    }

    public void setPivotRows(List<Integer> pivotRows) {
        this.pivotRows.clear();
        this.pivotRows.addAll(pivotRows);
    }

    public List<XLSXPivotTableConsolidator> getConsolidators() {
        return consolidators;
    }

    public void setConsolidators(List<XLSXPivotTableConsolidator> consolidators) {
        this.consolidators.clear();
        this.consolidators.addAll(consolidators);
    }

    @Override
    public String toString() {
        return "PivotTable [name=" + name + ", pivotColumns=" + pivotColumns + ", pivotRows=" + pivotRows
                + ", consolidators=" + consolidators + ", upperLeft=" + upperLeft + ", referenceTable=" + referenceTable
                + "]";
    }

}
