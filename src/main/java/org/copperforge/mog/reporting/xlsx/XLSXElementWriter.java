package org.copperforge.mog.reporting.xlsx;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.copperforge.mog.reporting.element.ElementWriter;
import org.copperforge.mog.reporting.element.ReportElement;

public abstract class XLSXElementWriter<T extends ReportElement> implements ElementWriter<T> {

    private XSSFWorkbook workbook;
    private XSSFSheet sheet;

    public XSSFWorkbook workbook() {
        return workbook;
    }

    public XSSFSheet sheet() {
        return sheet;
    }

    public XLSXElementWriter<T> workbook(XSSFWorkbook workbook) {
        this.workbook = workbook;
        
        return this;
    }

    public ElementWriter<T> sheet(XSSFSheet sheet) {
        this.sheet = sheet;
        return this;
    }
    
}
