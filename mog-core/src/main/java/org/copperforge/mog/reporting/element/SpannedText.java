package org.copperforge.mog.reporting.element;

import org.copperforge.mog.reporting.definition.CellReference;

public class SpannedText extends ReportElement {
    private String text;
    private CellReference upperLeft;
    private CellReference lowerRight;
    private String style;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public CellReference getUpperLeft() {
        return upperLeft;
    }

    public void setUpperLeft(CellReference upperRight) {
        this.upperLeft = upperRight;
    }

    public CellReference getLowerRight() {
        return lowerRight;
    }

    public void setLowerRight(CellReference lowerLeft) {
        this.lowerRight = lowerLeft;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        result = prime * result + ((upperLeft == null) ? 0 : upperLeft.hashCode());
        result = prime * result + ((lowerRight == null) ? 0 : lowerRight.hashCode());
        result = prime * result + ((style == null) ? 0 : style.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SpannedText other = (SpannedText) obj;
        if (text == null) {
            if (other.text != null)
                return false;
        } else if (!text.equals(other.text))
            return false;
        if (upperLeft == null) {
            if (other.upperLeft != null)
                return false;
        } else if (!upperLeft.equals(other.upperLeft))
            return false;
        if (lowerRight == null) {
            if (other.lowerRight != null)
                return false;
        } else if (!lowerRight.equals(other.lowerRight))
            return false;
        if (style == null) {
            if (other.style != null)
                return false;
        } else if (!style.equals(other.style))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SpannedText [text=" + text + ", upperLeft=" + upperLeft + ", lowerRight=" + lowerRight + ", style="
                + style + "]";
    }

}
