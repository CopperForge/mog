package org.copperforge.mog.reporting.xlsx;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.definition.CellReference;
import org.copperforge.mog.reporting.element.SpannedText;

public class StreamingXLSXSpannedTextWriter {

    private SXSSFSheet sheet;
    private XLSXStyleCache styles;

    public StreamingXLSXSpannedTextWriter sheet(SXSSFSheet sheet) {
        this.sheet = sheet;
        return this;
    }

    public StreamingXLSXSpannedTextWriter styles(XLSXStyleCache styles) {
        this.styles = styles;
        return this;
    }

    public void write(SpannedText textElement) throws MogException {
        validate(textElement);

        int firstRow = textElement.getUpperLeft().getRow() - 1;
        int lastRow = textElement.getLowerRight().getRow() - 1;
        int firstCol = textElement.getUpperLeft().getCol() - 1;
        int lastCol = textElement.getLowerRight().getCol() - 1;

        ensureRowsCanBeMutated(textElement, firstRow, lastRow);

        if (firstRow != lastRow || firstCol != lastCol) {
            sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
        }

        Row row = sheet.getRow(firstRow);
        if (row == null) {
            row = sheet.createRow(firstRow);
        }
        if (textElement.getHeight() != null) {
            row.setHeightInPoints(textElement.getHeight());
        }

        Cell cell = row.getCell(firstCol, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        if (styles != null) {
            org.apache.poi.ss.usermodel.CellStyle style = styles.cellStyle(textElement.getStyle());
            if (style != null) {
                cell.setCellStyle(style);
            }
        }
        cell.setCellValue(textElement.getText() != null ? textElement.getText() : "");
    }

    private void validate(SpannedText textElement) throws MogException {
        if (textElement.getUpperLeft() == null || textElement.getLowerRight() == null) {
            throw new MogException("Streaming spanned text requires upperLeft and lowerRight coordinates");
        }
        validateReference("upperLeft", textElement.getUpperLeft());
        validateReference("lowerRight", textElement.getLowerRight());
        if (textElement.getLowerRight().getRow() < textElement.getUpperLeft().getRow()
                || textElement.getLowerRight().getCol() < textElement.getUpperLeft().getCol()) {
            throw new MogException("Streaming spanned text lowerRight must be below and to the right of upperLeft");
        }
    }

    private void validateReference(String name, CellReference reference) throws MogException {
        if (reference.getRow() == null || reference.getCol() == null) {
            throw new MogException("Streaming spanned text " + name + " requires row and col (1-based)");
        }
        if (reference.getRow() < 1 || reference.getCol() < 1) {
            throw new MogException("Streaming spanned text " + name + " coordinates must be >= 1");
        }
    }

    private void ensureRowsCanBeMutated(SpannedText textElement, int firstRow, int lastRow) throws MogException {
        int lastFlushedRowNum = sheet.getLastFlushedRowNum();
        if (lastFlushedRowNum >= firstRow) {
            throw new MogException("XLSX streaming mode cannot write spanned text '" + textElement.getText()
                    + "' on rows " + (firstRow + 1) + "-" + (lastRow + 1)
                    + " because row " + (lastFlushedRowNum + 1)
                    + " or earlier has already been flushed by SXSSF");
        }
    }
}
