package org.copperforge.mog.reporting.xlsx;

import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;

import org.copperforge.mog.reporting.core.ReportException;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.element.ReportElement;
import org.copperforge.mog.reporting.element.SpannedText;

public class XLSXSpannedTextWriter extends XLSXElementWriter<SpannedText> {

    @Override
    public void write(Report report, ReportElement element) throws ReportException {
        XLSXReport xlsx = (XLSXReport) report;
        SpannedText textElement = (SpannedText) element;

        sheet().addMergedRegion(
                new CellRangeAddress(textElement.getUpperLeft().getRow(), textElement.getLowerRight().getRow(),
                        textElement.getUpperLeft().getCol(), textElement.getLowerRight().getCol()));

        Row row = sheet().createRow(textElement.getUpperLeft().getRow());
        row.setHeightInPoints(textElement.getHeight());

        Cell cell = row.createCell(textElement.getUpperLeft().getCol());
        Optional<XLSXStyle> cellStyle = xlsx.getStyles().stream().filter(s -> s.getName().equals(textElement.getStyle())).findFirst();
        if (cellStyle.isPresent()) cell.setCellStyle(((XLSXCellStyle) cellStyle.get()).xssfCellStyle(workbook()));
        cell.setCellValue(textElement.getText());
    }

}
