package org.copperforge.mog.reporting.xlsx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.definition.Report;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class XLSXOptionsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void existingXlsxReportJson_withoutOptions_deserializesWithNormalDefaults() throws Exception {
        Report report = objectMapper.readValue("""
                {
                  "name": "small",
                  "type": "xlsx",
                  "filename": "small.xlsx",
                  "sheets": []
                }
                """, Report.class);

        XLSXReport xlsxReport = assertInstanceOf(XLSXReport.class, report);
        XLSXOptions options = xlsxReport.getXlsx();

        assertFalse(options.isStreaming());
        assertEquals("normal", options.getMode());
        assertEquals(500, options.resolvedRowAccessWindowSize());
        assertTrue(options.resolvedCompressTempFiles());
        assertFalse(options.resolvedUseSharedStringsTable());
    }

    @Test
    void streamingMode_deserializesCorrectly() throws Exception {
        Report report = objectMapper.readValue("""
                {
                  "name": "large",
                  "type": "xlsx",
                  "filename": "large.xlsx",
                  "xlsx": {
                    "mode": "streaming",
                    "rowAccessWindowSize": 250,
                    "compressTempFiles": false,
                    "useSharedStringsTable": true
                  },
                  "sheets": []
                }
                """, Report.class);

        XLSXOptions options = assertInstanceOf(XLSXReport.class, report).getXlsx();

        assertTrue(options.isStreaming());
        assertEquals(250, options.resolvedRowAccessWindowSize());
        assertFalse(options.resolvedCompressTempFiles());
        assertTrue(options.resolvedUseSharedStringsTable());
    }

    @Test
    void invalidMode_failsClearly() {
        XLSXOptions options = new XLSXOptions();
        options.setMode("turbo");

        MogException ex = assertThrows(MogException.class, options::isStreaming);

        assertTrue(ex.getMessage().contains("Unsupported XLSX mode"));
        assertTrue(ex.getMessage().contains("turbo"));
    }
}
