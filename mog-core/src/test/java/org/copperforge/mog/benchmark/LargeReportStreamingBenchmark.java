package org.copperforge.mog.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.copperforge.mog.MogException;
import org.copperforge.mog.data.MogFetchCursor;
import org.copperforge.mog.data.MogFetchable;
import org.copperforge.mog.data.MogJdbcDataSource;
import org.copperforge.mog.data.filter.MogDataFilter;
import org.copperforge.mog.data.filter.MogQueryFilter;
import org.copperforge.mog.reporting.ReportDataSource;
import org.copperforge.mog.reporting.definition.CellReference;
import org.copperforge.mog.reporting.definition.Column;
import org.copperforge.mog.reporting.definition.Sheet;
import org.copperforge.mog.reporting.element.table.Table;
import org.copperforge.mog.reporting.xlsx.StreamingXLSXReportWriter;
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

        Runtime.getRuntime().gc();
        Thread.sleep(200);
        long heapAfterGc = usedHeap();

        print(config, dataSource.rowsRead(), elapsedMillis, fileSize, heapBefore, sampler.peakUsedHeap(),
                heapAfterGc, tempBefore, sampler.peakSxssfTempBytes(), tempAfter);
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
        report.setContext(MogContext.builder().build());
        report.setDataSources(List.of(dataSource));

        Sheet sheet = new Sheet();
        sheet.setName("Benchmark");
        sheet.setTitle("Benchmark");
        sheet.setElements(List.of(table(config)));
        report.setSheets(List.of(sheet));
        return report;
    }

    private static Table table(Config config) throws MogException {
        Table table = new Table();
        table.setType("table");
        table.setName("benchmark_table");

        CellReference upperLeft = new CellReference();
        upperLeft.setRow(1);
        upperLeft.setCol(1);
        table.setUpperLeft(upperLeft);

        List<Column> columns = new ArrayList<>();
        for (int i = 1; i <= config.columns(); i++) {
            columns.add(new Column("Column " + i, "c" + i));
        }
        table.setColumns(columns);

        ReportDataSource reportDataSource = new ReportDataSource();
        reportDataSource.setName("benchmark");
        MogQueryFilter filter = new MogQueryFilter(generatedQuery(config.rows(), config.columns()));
        filter.setType("query");
        reportDataSource.setFilter(filter);
        table.setDataSource(reportDataSource);
        return table;
    }

    private static String generatedQuery(long rows, int columns) {
        StringBuilder sql = new StringBuilder("select ");
        for (int i = 1; i <= columns; i++) {
            if (i > 1) {
                sql.append(", ");
            }
            sql.append("x + ").append(i - 1).append(" as c").append(i);
        }
        sql.append(" from system_range(1, ").append(rows).append(")");
        return sql.toString();
    }

    private static void print(Config config, long rowsWritten, long elapsedMillis, long fileSize,
            long heapBefore, long peakHeap, long heapAfterGc, long tempBefore, long peakTemp, long tempAfter) {
        double seconds = elapsedMillis / 1000.0;
        double rowsPerSecond = rowsWritten / seconds;

        System.out.println("MOG Large Report Streaming Benchmark");
        System.out.println("requestedRows=" + WHOLE.format(config.rows()));
        System.out.println("rowsActuallyWritten=" + WHOLE.format(rowsWritten));
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
        System.out.println("outputPath=" + config.outputPath().toAbsolutePath());
        System.out.println("Note: heap and temp/spool values are approximate developer diagnostics.");
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
        try (var stream = Files.walk(tempDir, 3)) {
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

    private record Config(long rows, int columns, int rowWindow, int fetchSize, Path outputPath) {
        static Config parse(String[] args) {
            long rows = 100000L;
            int columns = 10;
            int rowWindow = 100;
            int fetchSize = 1000;
            Path outputPath = null;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                String value = i + 1 < args.length ? args[++i] : "";
                switch (arg) {
                    case "--rows" -> rows = Long.parseLong(value);
                    case "--columns" -> columns = Integer.parseInt(value);
                    case "--rowWindow" -> rowWindow = Integer.parseInt(value);
                    case "--fetchSize" -> fetchSize = Integer.parseInt(value);
                    case "--outputPath" -> outputPath = Path.of(value);
                    default -> throw new IllegalArgumentException("Unsupported argument: " + arg);
                }
            }

            if (rows < 0) {
                throw new IllegalArgumentException("rows must be >= 0");
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
                outputPath = Path.of("build", "large-report-benchmark",
                        "mog-large-report-" + rows + "x" + columns + ".xlsx");
            }
            if (outputPath.getParent() == null) {
                outputPath = Path.of("build", "large-report-benchmark", outputPath.toString());
            }
            return new Config(rows, columns, rowWindow, fetchSize, outputPath);
        }
    }

    private static final class CountingJdbcDataSource extends MogJdbcDataSource {
        private final AtomicLong rowsRead = new AtomicLong();

        @Override
        public MogFetchCursor openCursor(MogDataFilter filter, MogContext context) throws MogException {
            MogFetchCursor delegate = super.openCursor(filter, context);
            return new MogFetchCursor() {
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
                    delegate.close();
                }
            };
        }

        long rowsRead() {
            return rowsRead.get();
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
