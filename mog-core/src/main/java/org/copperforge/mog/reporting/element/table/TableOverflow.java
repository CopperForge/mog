package org.copperforge.mog.reporting.element.table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TableOverflow {

    private String mode;
    private String sheetNamePattern;
    private Long maxDetailRowsPerSheet;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getSheetNamePattern() {
        return sheetNamePattern;
    }

    public void setSheetNamePattern(String sheetNamePattern) {
        this.sheetNamePattern = sheetNamePattern;
    }

    public Long getMaxDetailRowsPerSheet() {
        return maxDetailRowsPerSheet;
    }

    public void setMaxDetailRowsPerSheet(Long maxDetailRowsPerSheet) {
        this.maxDetailRowsPerSheet = maxDetailRowsPerSheet;
    }

    public boolean isNewSheet() {
        return mode != null && "newSheet".equalsIgnoreCase(mode);
    }

    @Override
    public String toString() {
        return "TableOverflow [mode=" + mode + ", sheetNamePattern=" + sheetNamePattern
                + ", maxDetailRowsPerSheet=" + maxDetailRowsPerSheet + "]";
    }
}
