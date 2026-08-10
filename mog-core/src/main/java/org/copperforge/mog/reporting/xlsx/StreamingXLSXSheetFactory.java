package org.copperforge.mog.reporting.xlsx;

import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.copperforge.mog.MogException;

interface StreamingXLSXSheetFactory {

    SXSSFSheet createContinuationSheet(String sheetName) throws MogException;
}
