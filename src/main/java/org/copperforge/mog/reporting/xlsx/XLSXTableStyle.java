package org.copperforge.mog.reporting.xlsx;

public class XLSXTableStyle extends XLSXStyle {

    private Boolean showColumnStripes = false;

    private Boolean showRowStripes = false;

    private Boolean showFirstColumn = true;

    private Boolean showLastColumn = true;

    public Boolean getShowColumnStripes() {
        return showColumnStripes;
    }

    public void setShowColumnStripes(Boolean showColumnStripes) {
        this.showColumnStripes = showColumnStripes;
    }

    public Boolean getShowRowStripes() {
        return showRowStripes;
    }

    public void setShowRowStripes(Boolean showRowStripes) {
        this.showRowStripes = showRowStripes;
    }

    public Boolean getShowFirstColumn() {
        return showFirstColumn;
    }

    public void setShowFirstColumn(Boolean showFirstColumn) {
        this.showFirstColumn = showFirstColumn;
    }

    public Boolean getShowLastColumn() {
        return showLastColumn;
    }

    public void setShowLastColumn(Boolean showLastColumn) {
        this.showLastColumn = showLastColumn;
    }

    @Override
    public String toString() {
        return "XLSXTableStyle [showColumnStripes=" + showColumnStripes + ", showRowStripes=" + showRowStripes
                + ", showFirstColumn=" + showFirstColumn + ", showLastColumn=" + showLastColumn + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((showColumnStripes == null) ? 0 : showColumnStripes.hashCode());
        result = prime * result + ((showRowStripes == null) ? 0 : showRowStripes.hashCode());
        result = prime * result + ((showFirstColumn == null) ? 0 : showFirstColumn.hashCode());
        result = prime * result + ((showLastColumn == null) ? 0 : showLastColumn.hashCode());
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
        XLSXTableStyle other = (XLSXTableStyle) obj;
        if (showColumnStripes == null) {
            if (other.showColumnStripes != null)
                return false;
        } else if (!showColumnStripes.equals(other.showColumnStripes))
            return false;
        if (showRowStripes == null) {
            if (other.showRowStripes != null)
                return false;
        } else if (!showRowStripes.equals(other.showRowStripes))
            return false;
        if (showFirstColumn == null) {
            if (other.showFirstColumn != null)
                return false;
        } else if (!showFirstColumn.equals(other.showFirstColumn))
            return false;
        if (showLastColumn == null) {
            if (other.showLastColumn != null)
                return false;
        } else if (!showLastColumn.equals(other.showLastColumn))
            return false;
        return true;
    }
    
}
