package org.copperforge.mog.reporting.xlsx;

import java.util.List;

import org.copperforge.mog.reporting.definition.Report;

public class XLSXReport extends Report {

    private List<XLSXStyle> styles;

    public List<XLSXStyle> getStyles() {
        return styles;
    }

    public void setStyles(List<XLSXStyle> styles) {
        this.styles = styles;
    }

}
