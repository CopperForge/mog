package org.copperforge.mog.reporting.xlsx;

import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class XLSXCellStyle extends XLSXStyle {

    private Boolean wrapText = true;

    private String verticalAlignment = "TOP";

    private String alignment;

    public Boolean getWrapText() {
        return wrapText;
    }

    public void setWrapText(Boolean wrapText) {
        this.wrapText = wrapText;
    }

    public String getVerticalAlignment() {
        return verticalAlignment;
    }

    public void setVerticalAlignment(String verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
    }

    public String getAlignment() {
        return alignment;
    }

    public void setAlignment(String alignment) {
        this.alignment = alignment;
    }

    public XSSFCellStyle xssfCellStyle(XSSFWorkbook workbook) {
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        if (getWrapText() != null)
            cellStyle.setWrapText(getWrapText());
        if (getVerticalAlignment() != null)
            cellStyle.setVerticalAlignment(VerticalAlignment.valueOf(getVerticalAlignment().toUpperCase()));
        if (getAlignment() != null)
            cellStyle.setAlignment(HorizontalAlignment.valueOf(getAlignment().toUpperCase()));

        return cellStyle;
    }

    @Override
    public String toString() {
        return "XLSXCellStyle [wrapText=" + wrapText + ", verticalAlignment=" + verticalAlignment + ", alignment="
                + alignment + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((wrapText == null) ? 0 : wrapText.hashCode());
        result = prime * result + ((verticalAlignment == null) ? 0 : verticalAlignment.hashCode());
        result = prime * result + ((alignment == null) ? 0 : alignment.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        XLSXCellStyle other = (XLSXCellStyle) obj;
        if (wrapText == null) {
            if (other.wrapText != null)
                return false;
        } else if (!wrapText.equals(other.wrapText))
            return false;
        if (verticalAlignment == null) {
            if (other.verticalAlignment != null)
                return false;
        } else if (!verticalAlignment.equals(other.verticalAlignment))
            return false;
        if (alignment == null) {
            if (other.alignment != null)
                return false;
        } else if (!alignment.equals(other.alignment))
            return false;
        return true;
    }

}
