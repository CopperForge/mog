package org.copperforge.mog.reporting.xlsx;

import java.util.List;

import org.copperforge.mog.reporting.definition.Report;

public class XLSXReport extends Report {

    private List<XLSXStyle> styles;

    private XLSXOptions xlsx = new XLSXOptions();

    public List<XLSXStyle> getStyles() {
        return styles;
    }

    public void setStyles(List<XLSXStyle> styles) {
        this.styles = styles;
    }

    public XLSXOptions getXlsx() {
        if (xlsx == null) {
            xlsx = new XLSXOptions();
        }
        return xlsx;
    }

    public void setXlsx(XLSXOptions xlsx) {
        this.xlsx = xlsx != null ? xlsx : new XLSXOptions();
    }

}
