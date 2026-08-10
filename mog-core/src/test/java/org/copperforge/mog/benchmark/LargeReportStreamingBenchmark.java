package org.copperforge.mog.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.copperforge.mog.MogException;
import org.copperforge.mog.data.MogFetchCursor;
import org.copperforge.mog.data.MogFetchable;
import org.copperforge.mog.data.MogJdbcDataSource;
import org.copperforge.mog.data.filter.MogDataFilter;
import org.copperforge.mog.data.filter.MogQueryFilter;
import org.copperforge.mog.data.filter.MogQueryParameter;
import org.copperforge.mog.reporting.ReportDataSource;
import org.copperforge.mog.reporting.definition.CellReference;
import org.copperforge.mog.reporting.definition.Column;
import org.copperforge.mog.reporting.definition.Sheet;
import org.copperforge.mog.reporting.element.table.Table;
import org.copperforge.mog.reporting.xlsx.StreamingXLSXReportWriter;
import org.copperforge.mog.reporting.xlsx.XLSXCellStyle;
import org.copperforge.mog.reporting.xlsx.XLSXOptions;
import org.copperforge.mog.reporting.xlsx.XLSXReport;
import org.copperforge.mog.runtime.MogContext;

public final class LargeReportStreamingBenchmark {

    private static final DecimalFormat WHOLE = new DecimalFormat("#,##0");
    private static final DecimalFormat DECIMAL = new DecimalFormat("#,##0.0");

    private LargeReportStreamingBenchmark() {
    }

    public static void main(String[] args) throws Exception {
        quietLogging();
        Config config = Config.parse(args);
        Files.createDirectories(config.outputPath().getParent());

        CountingJdbcDataSource dataSource = dataSource(config);
        XLSXReport report = report(config, dataSource);
        MemorySampler sampler = new MemorySampler(Duration.ofMillis(100));

        long heapBefore = usedHeap();
        long tempBefore = sxssfTempBytes();
        Instant started = Instant.now();

        sampler.start();
        try {
            StreamingXLSXReportWriter writer = new StreamingXLSXReportWriter();
            writer.build(report);
            writer.save(config.outputPath().toString());
        } finally {
            sampler.stop();
        }

        Instant finished = Instant.now();
        long elapsedMillis = Math.max(1, Duration.between(started, finished).toMillis());
        long fileSize = Files.size(config.outputPath());
        long tempAfter = sxssfTempBytes();
        ValidationResult validation = config.validate()
                ? validateWorkbook(config, config.outputPath())
                : ValidationResult.skipped();

        Runtime.getRuntime().gc();
        Thread.sleep(200);
        long heapAfterGc = usedHeap();

        print(config, dataSource, elapsedMillis, fileSize, heapBefore, sampler.peakUsedHeap(),
                heapAfterGc, tempBefore, sampler.peakSxssfTempBytes(), tempAfter, validation);

        if (config.deleteOutput()) {
            Files.deleteIfExists(config.outputPath());
            System.out.println("outputDeleted=true");
        }
    }

    private static void quietLogging() {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)
                org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.WARN);
    }

    private static CountingJdbcDataSource dataSource(Config config) {
        CountingJdbcDataSource dataSource = new CountingJdbcDataSource();
        dataSource.setName("benchmark");
        dataSource.setType("jdbc");
        dataSource.setUrl("jdbc:h2:mem:mog-large-report-benchmark;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword(null);
        dataSource.setFetchSize(config.fetchSize());
        return dataSource;
    }

    private static XLSXReport report(Config config, CountingJdbcDataSource dataSource) throws MogException {
        XLSXReport report = new XLSXReport();
        report.setName("large-report-benchmark");
        report.setType("xlsx");

        XLSXOptions options = new XLSXOptions();
        options.setMode("streaming");
        options.setRowAccessWindowSize(config.rowWindow());
        options.setCompressTempFiles(true);
        options.setUseSharedStringsTable(false);
        report.setXlsx(options);
        report.setStyles(List.of(headerStyle()));
        report.setContext(MogContext.builder().build());
        report.setDataSources(List.of(dataSource));

        List<Sheet> sheets = new ArrayList<>();
        for (int sheetNumber = 1; sheetNumber <= config.sheetCount(); sheetNumber++) {
            Sheet sheet = new Sheet();
            sheet.setName(sheetName(sheetNumber));
            sheet.setTitle(sheetName(sheetNumber));
            sheet.setElements(List.of(table(config, sheetNumber)));
            sheets.add(sheet);
        }
        report.setSheets(sheets);
        return report;
    }

    private static XLSXCellStyle headerStyle() {
        XLSXCellStyle style = new XLSXCellStyle();
        style.setType("cell");
        style.setName("benchmarkHeader");
        style.setAlignment("center");
        style.setVerticalAlignment("center");
        style.setWrapText(true);
        return style;
    }

    private static Table table(Config config, int sheetNumber) throws MogException {
        Table table = new Table();
        table.setType("table");
        table.setName("benchmark_table_" + sheetNumber);
        table.setStyle("benchmarkHeader");

        CellReference upperLeft = new CellReference();
        upperLeft.setRow(1);
        upperLeft.setCol(1);
        table.setUpperLeft(upperLeft);

        List<Column> columns = new ArrayList<>();
        for (int i = 1; i <= config.columns(); i++) {
            columns.add(new Column("Column " + i, "c" + i));
        }
        table.setColumns(columns);

        long rangeStart = ((long) sheetNumber - 1L) * config.rowsPerSheet() + 1L;
        long rangeEnd = rangeStart + config.rowsPerSheet() - 1L;
        ReportDataSource reportDataSource = new ReportDataSource();
        reportDataSource.setName("benchmark");
        MogQueryFilter filter = new MogQueryFilter(generatedQuery(config.columns()));
        filter.setType("query");
        Map<String, MogQueryParameter> parameters = new LinkedHashMap<>();
        parameters.put("rangeStart", new MogQueryParameter(rangeStart, "BIGINT"));
        parameters.put("rangeEnd", new MogQueryParameter(rangeEnd, "BIGINT"));
        filter.setParameters(parameters);
        reportDataSource.setFilter(filter);
        table.setDataSource(reportDataSource);
        return table;
    }

    private static String generatedQuery(int columns) {
        StringBuilder sql = new StringBuilder("select ");
        for (int i = 1; i <= columns; i++) {
            if (i > 1) {
                sql.append(", ");
            }
            sql.append("x + ").append(i - 1).append(" as c").append(i);
        }
        sql.append(" from system_range(:rangeStart, :rangeEnd)");
        return sql.toString();
    }

    private static ValidationResult validateWorkbook(Config config, Path outputPath) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(outputPath.toFile())) {
            if (workbook.getNumberOfSheets() != config.sheetCount()) {
                throw new IllegalStateException("Expected " + config.sheetCount() + " sheets but found "
                        + workbook.getNumberOfSheets());
            }
            for (int sheetNumber : representativeSheetNumbers(config.sheetCount())) {
                validateSheet(config, workbook, sheetNumber);
            }
            return new ValidationResult(true, workbook.getNumCellStyles(), representativeSheetNumbers(config.sheetCount()));
        }
    }

    private static void validateSheet(Config config, XSSFWorkbook workbook, int sheetNumber) {
        XSSFSheet sheet = workbook.getSheet(sheetName(sheetNumber));
        if (sheet == null) {
            throw new IllegalStateException("Missing sheet " + sheetName(sheetNumber));
        }
        String finalColumn = org.apache.poi.ss.util.CellReference.convertNumToColString(config.columns() - 1);
        String expectedFilter = "A1:" + finalColumn + (config.rowsPerSheet() + 1);
        String actualFilter = sheet.getCTWorksheet().getAutoFilter().getRef();
        if (!expectedFilter.equals(actualFilter)) {
            throw new IllegalStateException("Expected autofilter " + expectedFilter + " on " + sheet.getSheetName()
                    + " but found " + actualFilter);
        }
        if (!"Column 1".equals(sheet.getRow(0).getCell(0).getStringCellValue())) {
            throw new IllegalStateException("Unexpected first header on " + sheet.getSheetName());
        }
        if (!("Column " + config.columns()).equals(sheet.getRow(0).getCell(config.columns() - 1).getStringCellValue())) {
            throw new IllegalStateException("Unexpected final header on " + sheet.getSheetName());
        }
        if (config.rowsPerSheet() > 0) {
            validateValue(config, sheet, sheetNumber, 1L);
            validateValue(config, sheet, sheetNumber, Math.max(1L, config.rowsPerSheet() / 2L));
            validateValue(config, sheet, sheetNumber, config.rowsPerSheet());
        }
    }

    private static void validateValue(Config config, org.apache.poi.ss.usermodel.Sheet sheet,
            int sheetNumber, long detailRowNumber) {
        long expected = ((long) sheetNumber - 1L) * config.rowsPerSheet() + detailRowNumber;
        int rowIndex = Math.toIntExact(detailRowNumber);
        double actual = sheet.getRow(rowIndex).getCell(0).getNumericCellValue();
        if (Double.compare(actual, expected) != 0) {
            throw new IllegalStateException("Expected " + expected + " at " + sheet.getSheetName()
                    + " row " + (rowIndex + 1) + " but found " + actual);
        }
    }

    private static List<Integer> representativeSheetNumbers(int sheetCount) {
        if (sheetCount <= 3) {
            List<Integer> all = new ArrayList<>();
            for (int i = 1; i <= sheetCount; i++) {
                all.add(i);
            }
            return all;
        }
        List<Integer> representatives = new ArrayList<>();
        representatives.add(1);
        int middle = Math.max(1, (sheetCount + 1) / 2);
        if (!representatives.contains(middle)) {
            representatives.add(middle);
        }
        if (!representatives.contains(sheetCount)) {
            representatives.add(sheetCount);
        }
        return representatives;
    }

    private static void print(Config config, CountingJdbcDataSource dataSource, long elapsedMillis, long fileSize,
            long heapBefore, long peakHeap, long heapAfterGc, long tempBefore, long peakTemp, long tempAfter,
            ValidationResult validation) {
        double seconds = elapsedMillis / 1000.0;
        double rowsPerSecond = dataSource.rowsRead() / seconds;

        System.out.println("MOG Large Report Streaming Benchmark");
        System.out.println("requestedRows=" + WHOLE.format(config.totalRows()));
        System.out.println("totalDetailRows=" + WHOLE.format(config.totalRows()));
        System.out.println("rowsActuallyWritten=" + WHOLE.format(dataSource.rowsRead()));
        System.out.println("sheetCount=" + WHOLE.format(config.sheetCount()));
        System.out.println("rowsPerSheet=" + WHOLE.format(config.rowsPerSheet()));
        System.out.println("columns=" + WHOLE.format(config.columns()));
        System.out.println("sxssfRowAccessWindowSize=" + WHOLE.format(config.rowWindow()));
        System.out.println("jdbcFetchSize=" + WHOLE.format(config.fetchSize()));
        System.out.println("elapsedSeconds=" + DECIMAL.format(seconds));
        System.out.println("rowsPerSecond=" + WHOLE.format(Math.round(rowsPerSecond)));
        System.out.println("xlsxFileSizeBytes=" + WHOLE.format(fileSize));
        System.out.println("xlsxFileSizeMiB=" + DECIMAL.format(fileSize / 1024.0 / 1024.0));
        System.out.println("usedHeapBeforeMiBApprox=" + DECIMAL.format(toMiB(heapBefore)));
        System.out.println("peakUsedHeapMiBApproxSampled=" + DECIMAL.format(toMiB(peakHeap)));
        System.out.println("usedHeapAfterGcMiBApprox=" + DECIMAL.format(toMiB(heapAfterGc)));
        System.out.println("sxssfTempBytesBeforeApprox=" + WHOLE.format(tempBefore));
        System.out.println("sxssfTempBytesPeakApproxSampled=" + WHOLE.format(peakTemp));
        System.out.println("sxssfTempBytesAfterApprox=" + WHOLE.format(tempAfter));
        System.out.println("jdbcCursorOpenCount=" + WHOLE.format(dataSource.openCursorCalls()));
        System.out.println("jdbcCursorCloseCount=" + WHOLE.format(dataSource.closeCursorCalls()));
        System.out.println("jdbcMaxConcurrentCursors=" + WHOLE.format(dataSource.maxConcurrentCursors()));
        System.out.println("jdbcOverlappingCursorOpens=" + WHOLE.format(dataSource.overlappingCursorOpens()));
        System.out.println("validationPerformed=" + validation.performed());
        System.out.println("validatedSheetNumbers=" + validation.validatedSheets());
        System.out.println("validatedWorkbookStyleCount=" + validation.styleCountText());
        System.out.println("outputPath=" + config.outputPath().toAbsolutePath());
        System.out.println("sheetBoundarySnapshots:");
        for (SheetBoundarySnapshot snapshot : dataSource.snapshots()) {
            System.out.println("  sheet=" + snapshot.sheetNumber()
                    + " rowsRead=" + WHOLE.format(snapshot.rowsRead())
                    + " usedHeapMiBApprox=" + DECIMAL.format(toMiB(snapshot.usedHeapBytes()))
                    + " sxssfTempBytesApprox=" + WHOLE.format(snapshot.sxssfTempBytes()));
        }
        System.out.println("Note: heap and temp/spool values are approximate developer diagnostics.");
    }

    private static String sheetName(int sheetNumber) {
        return String.format(Locale.ROOT, "Benchmark %02d", sheetNumber);
    }

    private static long usedHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static double toMiB(long bytes) {
        return bytes / 1024.0 / 1024.0;
    }

    private static long sxssfTempBytes() {
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
        if (!Files.isDirectory(tempDir)) {
            return 0;
        }
        try (java.util.stream.Stream<Path> stream = Files.walk(tempDir, 3)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).contains("sxssf"))
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            return 0;
        }
    }

    private record Config(int sheetCount, long rowsPerSheet, int columns, int rowWindow, int fetchSize,
            Path outputPath, boolean validate, boolean deleteOutput) {

        long totalRows() {
            return rowsPerSheet * sheetCount;
        }

        static Config parse(String[] args) {
            long rows = 100000L;
            Long rowsPerSheet = null;
            int sheetCount = 1;
            int columns = 10;
            int rowWindow = 100;
            int fetchSize = 1000;
            boolean validate = false;
            boolean deleteOutput = false;
            Path outputPath = null;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                String value = i + 1 < args.length ? args[++i] : "";
                switch (arg) {
                    case "--rows" -> rows = Long.parseLong(value);
                    case "--rowsPerSheet" -> rowsPerSheet = Long.parseLong(value);
                    case "--sheetCount" -> sheetCount = Integer.parseInt(value);
                    case "--columns" -> columns = Integer.parseInt(value);
                    case "--rowWindow" -> rowWindow = Integer.parseInt(value);
                    case "--fetchSize" -> fetchSize = Integer.parseInt(value);
                    case "--validate" -> validate = Boolean.parseBoolean(value);
                    case "--deleteOutput" -> deleteOutput = Boolean.parseBoolean(value);
                    case "--outputPath" -> outputPath = Path.of(value);
                    default -> throw new IllegalArgumentException("Unsupported argument: " + arg);
                }
            }

            long effectiveRowsPerSheet = rowsPerSheet != null ? rowsPerSheet : rows;
            if (sheetCount < 1) {
                throw new IllegalArgumentException("sheetCount must be >= 1");
            }
            if (effectiveRowsPerSheet < 0) {
                throw new IllegalArgumentException("rowsPerSheet must be >= 0");
            }
            if (effectiveRowsPerSheet > 1_048_575L) {
                throw new IllegalArgumentException("rowsPerSheet must fit below the XLSX detail-row limit");
            }
            if (columns < 1) {
                throw new IllegalArgumentException("columns must be >= 1");
            }
            if (rowWindow == 0 || rowWindow < -1) {
                throw new IllegalArgumentException("rowWindow must be greater than 0 or -1");
            }
            if (fetchSize < 0) {
                throw new IllegalArgumentException("fetchSize must be >= 0");
            }
            if (outputPath == null) {
                if (sheetCount == 1) {
                    outputPath = Path.of("build", "large-report-benchmark",
                            "mog-large-report-" + effectiveRowsPerSheet + "x" + columns + ".xlsx");
                } else {
                    outputPath = Path.of("build", "large-report-benchmark",
                            "mog-large-report-" + sheetCount + "sheets-" + effectiveRowsPerSheet
                                    + "x" + columns + ".xlsx");
                }
            }
            if (outputPath.getParent() == null) {
                outputPath = Path.of("build", "large-report-benchmark", outputPath.toString());
            }
            return new Config(sheetCount, effectiveRowsPerSheet, columns, rowWindow, fetchSize,
                    outputPath, validate, deleteOutput);
        }
    }

    private static final class CountingJdbcDataSource extends MogJdbcDataSource {
        private final AtomicLong rowsRead = new AtomicLong();
        private final AtomicInteger openCursorCalls = new AtomicInteger();
        private final AtomicInteger closeCursorCalls = new AtomicInteger();
        private final AtomicInteger activeCursors = new AtomicInteger();
        private final AtomicInteger maxConcurrentCursors = new AtomicInteger();
        private final AtomicInteger overlappingCursorOpens = new AtomicInteger();
        private final List<SheetBoundarySnapshot> snapshots = Collections.synchronizedList(new ArrayList<>());

        @Override
        public MogFetchCursor openCursor(MogDataFilter filter, MogContext context) throws MogException {
            int sheetNumber = openCursorCalls.incrementAndGet();
            int activeBeforeOpen = activeCursors.getAndIncrement();
            if (activeBeforeOpen > 0) {
                overlappingCursorOpens.incrementAndGet();
            }
            maxConcurrentCursors.accumulateAndGet(activeBeforeOpen + 1, Math::max);

            MogFetchCursor delegate = super.openCursor(filter, context);
            return new MogFetchCursor() {
                private boolean closed = false;

                @Override
                public List<String> columns() throws MogException {
                    return delegate.columns();
                }

                @Override
                public boolean next() throws MogException {
                    boolean hasNext = delegate.next();
                    if (hasNext) {
                        rowsRead.incrementAndGet();
                    }
                    return hasNext;
                }

                @Override
                public MogFetchable current() throws MogException {
                    return delegate.current();
                }

                @Override
                public long rowNumber() {
                    return delegate.rowNumber();
                }

                @Override
                public void close() throws MogException {
                    if (closed) {
                        return;
                    }
                    closed = true;
                    try {
                        delegate.close();
                    } finally {
                        closeCursorCalls.incrementAndGet();
                        activeCursors.decrementAndGet();
                        snapshots.add(new SheetBoundarySnapshot(sheetNumber, rowsRead.get(), usedHeap(), sxssfTempBytes()));
                    }
                }
            };
        }

        long rowsRead() {
            return rowsRead.get();
        }

        int openCursorCalls() {
            return openCursorCalls.get();
        }

        int closeCursorCalls() {
            return closeCursorCalls.get();
        }

        int maxConcurrentCursors() {
            return maxConcurrentCursors.get();
        }

        int overlappingCursorOpens() {
            return overlappingCursorOpens.get();
        }

        List<SheetBoundarySnapshot> snapshots() {
            return List.copyOf(snapshots);
        }
    }

    private record SheetBoundarySnapshot(int sheetNumber, long rowsRead, long usedHeapBytes, long sxssfTempBytes) {
    }

    private record ValidationResult(boolean performed, int styleCount, List<Integer> validatedSheets) {
        static ValidationResult skipped() {
            return new ValidationResult(false, -1, List.of());
        }

        String styleCountText() {
            return performed ? WHOLE.format(styleCount) : "skipped";
        }
    }

    private static final class MemorySampler implements Runnable {
        private final Duration interval;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final AtomicLong peakUsedHeap = new AtomicLong(usedHeap());
        private final AtomicLong peakSxssfTempBytes = new AtomicLong(sxssfTempBytes());
        private Thread thread;

        private MemorySampler(Duration interval) {
            this.interval = interval;
        }

        void start() {
            running.set(true);
            thread = new Thread(this, "mog-large-report-benchmark-sampler");
            thread.setDaemon(true);
            thread.start();
        }

        void stop() throws InterruptedException {
            running.set(false);
            if (thread != null) {
                thread.join(interval.toMillis() * 3);
            }
            sample();
        }

        long peakUsedHeap() {
            return peakUsedHeap.get();
        }

        long peakSxssfTempBytes() {
            return peakSxssfTempBytes.get();
        }

        @Override
        public void run() {
            while (running.get()) {
                sample();
                try {
                    Thread.sleep(interval.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running.set(false);
                }
            }
        }

        private void sample() {
            peakUsedHeap.accumulateAndGet(usedHeap(), Math::max);
            peakSxssfTempBytes.accumulateAndGet(sxssfTempBytes(), Math::max);
        }
    }
}
