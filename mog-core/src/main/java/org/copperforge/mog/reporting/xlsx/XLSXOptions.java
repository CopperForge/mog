package org.copperforge.mog.reporting.xlsx;

import org.copperforge.mog.MogException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class XLSXOptions {

    public static final String MODE_NORMAL = "normal";
    public static final String MODE_STREAMING = "streaming";

    private String mode = MODE_NORMAL;

    private Integer rowAccessWindowSize = 500;

    private Boolean compressTempFiles = true;

    private Boolean useSharedStringsTable = false;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Integer getRowAccessWindowSize() {
        return rowAccessWindowSize;
    }

    public void setRowAccessWindowSize(Integer rowAccessWindowSize) {
        this.rowAccessWindowSize = rowAccessWindowSize;
    }

    public Boolean getCompressTempFiles() {
        return compressTempFiles;
    }

    public void setCompressTempFiles(Boolean compressTempFiles) {
        this.compressTempFiles = compressTempFiles;
    }

    public Boolean getUseSharedStringsTable() {
        return useSharedStringsTable;
    }

    public void setUseSharedStringsTable(Boolean useSharedStringsTable) {
        this.useSharedStringsTable = useSharedStringsTable;
    }

    public boolean isStreaming() throws MogException {
        String resolved = resolvedMode();
        if (MODE_NORMAL.equals(resolved)) {
            return false;
        }
        if (MODE_STREAMING.equals(resolved)) {
            return true;
        }
        throw new MogException("Unsupported XLSX mode '" + mode + "'");
    }

    public int resolvedRowAccessWindowSize() {
        return rowAccessWindowSize != null ? rowAccessWindowSize : 500;
    }

    public boolean resolvedCompressTempFiles() {
        return compressTempFiles == null || compressTempFiles;
    }

    public boolean resolvedUseSharedStringsTable() {
        return Boolean.TRUE.equals(useSharedStringsTable);
    }

    private String resolvedMode() {
        if (mode == null || mode.isBlank()) {
            return MODE_NORMAL;
        }
        return mode.trim().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public String toString() {
        return "XLSXOptions [mode=" + mode + ", rowAccessWindowSize=" + rowAccessWindowSize
                + ", compressTempFiles=" + compressTempFiles + ", useSharedStringsTable="
                + useSharedStringsTable + "]";
    }
}
