package org.copperforge.mog.reporting.xlsx;

import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.element.ReportElement;
import org.copperforge.mog.reporting.element.SpannedText;

public class XLSXSpannedTextWriter extends XLSXElementWriter<SpannedText> {

    @Override
    public void write(Report report, ReportElement element) throws MogException {
        XLSXReport xlsx = (XLSXReport) report;
        SpannedText textElement = (SpannedText) element;

        sheet().addMergedRegion(
                new CellRangeAddress(textElement.getUpperLeft().getRow() - 1, textElement.getLowerRight().getRow() - 1,
                        textElement.getUpperLeft().getCol() - 1, textElement.getLowerRight().getCol() - 1));

        Row row = sheet().createRow(textElement.getUpperLeft().getRow() - 1);
        row.setHeightInPoints(textElement.getHeight());

        Cell cell = row.createCell(textElement.getUpperLeft().getCol() - 1);
        Optional<XLSXStyle> cellStyle = xlsx.getStyles().stream().filter(s -> s.getName().equals(textElement.getStyle())).findFirst();
        if (cellStyle.isPresent()) cell.setCellStyle(((XLSXCellStyle) cellStyle.get()).xssfCellStyle(workbook()));
        cell.setCellValue(textElement.getText());
    }

}
