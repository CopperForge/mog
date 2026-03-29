package org.copperforge.mog.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.definition.CellReference;
import org.copperforge.mog.reporting.definition.Sheet;
import org.copperforge.mog.reporting.element.SpannedText;
import org.copperforge.mog.reporting.xlsx.XLSXReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MogRuntimeReportGenerationTest {

    @TempDir
    Path tempDir;

    @Test
    void generateReport_createsWorkbook_withoutPriorReportParsing() throws Exception {
        Path output = tempDir.resolve("single.xlsx");

        String saved = MogRuntime.generateReport(report(output, "single"), MogContext.builder().build());

        assertEquals(output.toString(), saved);
        assertTrue(Files.exists(output));
        assertWorkbookCell(output, "single");
    }

    @Test
    void generateReport_supportsConcurrentInvocations() throws Exception {
        int count = 6;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(count);
        try {
            List<Callable<Path>> tasks = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Path output = tempDir.resolve("concurrent-" + i + ".xlsx");
                String text = "sheet-" + i;
                tasks.add(() -> {
                    start.await();
                    String saved = MogRuntime.generateReport(report(output, text), MogContext.builder().build());
                    return Path.of(saved);
                });
            }

            List<Future<Path>> futures = new ArrayList<>();
            for (Callable<Path> task : tasks) {
                futures.add(executor.submit(task));
            }
            start.countDown();

            for (int i = 0; i < futures.size(); i++) {
                Path saved = futures.get(i).get();
                assertTrue(Files.exists(saved));
                assertWorkbookCell(saved, "sheet-" + i);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private XLSXReport report(Path output, String text) throws MogException {
        XLSXReport report = new XLSXReport();
        report.setName("test");
        report.setType("xlsx");
        report.setFilename(output.toString());
        report.setStyles(List.of());

        Sheet sheet = new Sheet();
        sheet.setName("S1");
        sheet.setTitle("Sheet1");
        sheet.setElements(List.of(spannedText(text)));
        report.setSheets(List.of(sheet));
        return report;
    }

    private SpannedText spannedText(String text) {
        SpannedText element = new SpannedText();
        element.setType("spannedText");
        element.setText(text);
        element.setHeight(12);
        element.setUpperLeft(cell(1, 1));
        element.setLowerRight(cell(1, 2));
        return element;
    }

    private CellReference cell(int row, int col) {
        CellReference cell = new CellReference();
        cell.setRow(row);
        cell.setCol(col);
        return cell;
    }

    private void assertWorkbookCell(Path workbookPath, String expected) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(workbookPath.toFile())) {
            assertNotNull(workbook.getSheet("Sheet1"));
            assertEquals(expected, workbook.getSheet("Sheet1").getRow(0).getCell(0).getStringCellValue());
        }
    }
}
