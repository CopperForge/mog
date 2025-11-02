package org.copperforge.mog.reporting.xlsx;

import java.io.FileOutputStream;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.definition.Sheet;
import org.copperforge.mog.reporting.element.ReportElement;
import org.copperforge.mog.reporting.writer.AbstractReportWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XLSXReportWriter extends AbstractReportWriter {

    private Logger log = LoggerFactory.getLogger(XLSXReportWriter.class);
    
    private XSSFWorkbook workbook = null;

    public XLSXReportWriter() {
        super("xlsx");
        addElementWriter("table", new XLSXTableWriter());
        addElementWriter("spannedText", new XLSXSpannedTextWriter());
        addElementWriter("pivotTable", new XLSXPivotTableWriter());
    }

    @Override
    protected void buildReport(Report report) throws MogException {
        workbook = new XSSFWorkbook();
        super.buildReport(report);
    }    

    @Override
    protected void buildSheet(Report report, Sheet sheet) throws MogException {
        log.info("Building sheet '" + sheet.getTitle() + "'");
        XSSFSheet xlsxSheet = workbook.createSheet(sheet.getTitle());
        for (ReportElement element : sheet.getElements()) {
             XLSXElementWriter<?> writer = ((XLSXElementWriter<?>) (elementWriters().get(element.getType())));
             writer.workbook(workbook).sheet(xlsxSheet).write(report, element);
        }
    }

    @Override
    public void save(String filename) throws MogException {
        if (workbook == null) throw new MogException("The report must be built before it can be saved");
        try {
            try (FileOutputStream outputStream = new FileOutputStream(filename)) {
                workbook.write(outputStream);
            }
        } catch (Exception e) {
            throw new MogException(e);
        } finally {
            try { workbook.close(); } catch (Exception ignore) {}
            workbook = null;
        }
    }

}
