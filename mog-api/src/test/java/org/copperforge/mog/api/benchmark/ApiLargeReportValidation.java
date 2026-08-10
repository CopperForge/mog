package org.copperforge.mog.api.benchmark;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.copperforge.mog.api.MogApiApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ApiLargeReportValidation {

    private static final DecimalFormat WHOLE = new DecimalFormat("#,##0");
    private static final DecimalFormat DECIMAL = new DecimalFormat("#,##0.0");
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    private ApiLargeReportValidation() {
    }

    public static void main(String[] args) throws Exception {
        quietLogging();
        Config config = Config.parse(args);
        Path workDir = Path.of("build", "api-large-report-validation").toAbsolutePath().normalize();
        Path storeDir = workDir.resolve("store");
        recreateDirectory(storeDir);

        CountingDriverMetrics.reset();
        try (ConfigurableApplicationContext context = startApi(storeDir, config)) {
            Environment environment = context.getEnvironment();
            URI baseUri = URI.create("http://localhost:" + environment.getProperty("local.server.port") + "/api");
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

            ValidationResult smallValidation = runSmallWorkbookValidation(http, baseUri, storeDir, config);
            CountingDriverMetrics.reset();
            RunResult result = runProductionShape(http, baseUri, storeDir, config);
            print(config, result, smallValidation);

            if (config.deleteOutput() && result.artifactPath() != null) {
                Files.deleteIfExists(result.artifactPath());
                System.out.println("artifactDeleted=true");
            }
        }
    }

    private static ConfigurableApplicationContext startApi(Path storeDir, Config config) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("server.port", 0);
        properties.put("debug", false);
        properties.put("spring.main.banner-mode", "off");
        properties.put("mog.api.store-dir", storeDir.toString());
        properties.put("mog.api.runs.workers", config.workers());
        properties.put("mog.api.runs.queue-capacity", config.queueCapacity());
        properties.put("logging.level.root", "WARN");
        properties.put("logging.level.org.springframework", "WARN");
        return new SpringApplicationBuilder(MogApiApplication.class)
                .web(WebApplicationType.SERVLET)
                .properties(properties)
                .run();
    }

    private static ValidationResult runSmallWorkbookValidation(HttpClient http, URI baseUri, Path storeDir,
            Config config) throws Exception {
        if (config.smallValidationRows() <= 0 || config.smallValidationSheets() <= 0) {
            return ValidationResult.skipped();
        }
        Shape shape = Shape.even(config.smallValidationRows() * (long) config.smallValidationSheets(),
                config.smallValidationSheets());
        String reportId = "small-api-validation-report";
        String datasourceId = "small-api-validation-datasource";
        writeSyntheticDsl(storeDir, reportId, datasourceId, shape, config);
        RunObservation observation = submitAndWait(http, baseUri, reportId, datasourceId, false);
        Path artifact = artifactPath(storeDir, observation.runId());
        int styleCount;
        try (XSSFWorkbook workbook = new XSSFWorkbook(artifact.toFile())) {
            if (workbook.getNumberOfSheets() != shape.sheetRows().size()) {
                throw new IllegalStateException("Expected " + shape.sheetRows().size() + " small validation sheets");
            }
            styleCount = workbook.getNumCellStyles();
            for (int sheet = 1; sheet <= shape.sheetRows().size(); sheet++) {
                var xssfSheet = workbook.getSheet(sheetName(sheet));
                if (xssfSheet == null) {
                    throw new IllegalStateException("Missing small validation sheet " + sheetName(sheet));
                }
                if (!"Synthetic Orion Cycle Detail".equals(xssfSheet.getRow(0).getCell(0).getStringCellValue())) {
                    throw new IllegalStateException("Missing spanned title on " + sheetName(sheet));
                }
                if (!"Column 1".equals(xssfSheet.getRow(2).getCell(0).getStringCellValue())) {
                    throw new IllegalStateException("Missing table header on " + sheetName(sheet));
                }
                long rows = shape.sheetRows().get(sheet - 1);
                if (rows > 0) {
                    double first = xssfSheet.getRow(3).getCell(0).getNumericCellValue();
                    double last = xssfSheet.getRow(Math.toIntExact(rows + 2)).getCell(0).getNumericCellValue();
                    if (first < 1 || last < first) {
                        throw new IllegalStateException("Unexpected data values on " + sheetName(sheet));
                    }
                }
            }
        } finally {
            Files.deleteIfExists(artifact);
        }
        return new ValidationResult(true, styleCount, observation.transitions());
    }

    private static RunResult runProductionShape(HttpClient http, URI baseUri, Path storeDir, Config config)
            throws Exception {
        String reportId = "production-shape-report";
        String datasourceId = "production-shape-datasource";
        Shape shape = Shape.even(config.totalRows(), config.sheetCount());
        writeSyntheticDsl(storeDir, reportId, datasourceId, shape, config);

        MemorySampler sampler = new MemorySampler(Duration.ofMillis(100));
        long tempBefore = sxssfTempBytes();
        long heapBefore = usedHeap();
        sampler.start();
        RunObservation observation;
        try {
            observation = submitAndWait(http, baseUri, reportId, datasourceId, true);
        } finally {
            sampler.stop();
        }

        Path artifact = artifactPath(storeDir, observation.runId());
        long artifactSize = Files.size(artifact);
        ZipValidation zipValidation = config.validateZip() ? validateZip(artifact, config.sheetCount()) : ZipValidation.skipped();
        int styleCount = config.validateZip() ? readStyleCount(artifact) : -1;
        long tempAfterCompletion = sxssfTempBytes();
        HttpResponse<InputStream> artifactResponse = artifactGet(http, baseUri, observation.runId());
        long firstBytes;
        try (InputStream input = artifactResponse.body()) {
            byte[] buffer = input.readNBytes(4096);
            firstBytes = buffer.length;
        }

        Runtime.getRuntime().gc();
        Thread.sleep(200);
        long heapAfterGc = usedHeap();

        return new RunResult(observation.runId(), observation.postResponseMillis(), observation.transitions(),
                observation.elapsedMillis(), artifact, artifactSize, heapBefore, sampler.peakUsedHeap(),
                heapAfterGc, tempBefore, sampler.peakSxssfTempBytes(), tempAfterCompletion,
                zipValidation, styleCount, artifactResponse.statusCode(),
                artifactResponse.headers().firstValueAsLong("content-length").orElse(-1L), firstBytes);
    }

    private static RunObservation submitAndWait(HttpClient http, URI baseUri, String reportId, String datasourceId,
            boolean verifyArtifactConflict) throws Exception {
        String body = """
                {
                  "reportId": "%s",
                  "datasourceId": "%s",
                  "params": {},
                  "output": { "format": "xlsx" }
                }
                """.formatted(reportId, datasourceId);
        Instant beforePost = Instant.now();
        HttpResponse<String> post = http.send(HttpRequest.newBuilder(baseUri.resolve("/api/runs"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
        long postMillis = Duration.between(beforePost, Instant.now()).toMillis();
        if (post.statusCode() != 202) {
            throw new IllegalStateException("POST /api/runs returned " + post.statusCode() + ": " + post.body());
        }
        String runId = JSON.readTree(post.body()).get("runId").asText();
        List<String> transitions = new ArrayList<>();
        Instant started = Instant.now();
        String previous = null;
        boolean conflictVerified = false;
        while (true) {
            HttpResponse<String> get = http.send(HttpRequest.newBuilder(baseUri.resolve("/api/runs/" + runId))
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            if (get.statusCode() != 200) {
                throw new IllegalStateException("GET run returned " + get.statusCode() + ": " + get.body());
            }
            JsonNode metadata = JSON.readTree(get.body());
            String status = metadata.get("status").asText();
            if (!status.equals(previous)) {
                transitions.add(status);
                previous = status;
            }
            if (verifyArtifactConflict && !conflictVerified && !"COMPLETED".equals(status) && !"FAILED".equals(status)) {
                HttpResponse<String> artifact = http.send(HttpRequest.newBuilder(baseUri.resolve("/api/runs/" + runId + "/artifact"))
                        .GET().build(), HttpResponse.BodyHandlers.ofString());
                if (artifact.statusCode() != 409) {
                    throw new IllegalStateException("Expected artifact conflict before completion, got "
                            + artifact.statusCode());
                }
                conflictVerified = true;
            }
            if ("COMPLETED".equals(status)) {
                return new RunObservation(runId, postMillis, transitions,
                        Duration.between(started, Instant.now()).toMillis());
            }
            if ("FAILED".equals(status)) {
                throw new IllegalStateException("Run failed: " + get.body());
            }
            Thread.sleep(1000);
        }
    }

    private static HttpResponse<InputStream> artifactGet(HttpClient http, URI baseUri, String runId) throws Exception {
        HttpResponse<InputStream> response = http.send(HttpRequest.newBuilder(baseUri.resolve("/api/runs/" + runId + "/artifact"))
                .GET().build(), HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Artifact GET returned " + response.statusCode());
        }
        return response;
    }

    private static void writeSyntheticDsl(Path storeDir, String reportId, String datasourceId, Shape shape,
            Config config) throws IOException {
        Files.createDirectories(storeDir.resolve("reports"));
        Files.createDirectories(storeDir.resolve("datasources"));
        Files.writeString(storeDir.resolve("datasources").resolve(datasourceId + ".json"), datasourceJson(datasourceId, config),
                StandardCharsets.UTF_8);
        Files.writeString(storeDir.resolve("reports").resolve(reportId + ".json"), reportJson(reportId, datasourceId, shape, config),
                StandardCharsets.UTF_8);
    }

    private static String datasourceJson(String datasourceId, Config config) {
        return """
                {
                  "id": "%s",
                  "datasources": [
                    {
                      "name": "synthetic",
                      "type": "jdbc",
                      "url": "jdbc:mogbench:h2:mem:mog-api-production-shape;DB_CLOSE_DELAY=-1",
                      "jdbcClass": "%s",
                      "user": "sa",
                      "password": "",
                      "fetchSize": %d
                    }
                  ]
                }
                """.formatted(datasourceId, CountingDriver.class.getName(), config.fetchSize());
    }

    private static String reportJson(String reportId, String datasourceId, Shape shape, Config config) {
        StringBuilder sheets = new StringBuilder();
        long start = 1L;
        for (int i = 0; i < shape.sheetRows().size(); i++) {
            if (i > 0) {
                sheets.append(",\n");
            }
            long rows = shape.sheetRows().get(i);
            long end = start + rows - 1;
            sheets.append(sheetJson(i + 1, start, end, rows, config.columns()));
            start = end + 1;
        }
        return """
                {
                  "id": "%s",
                  "name": "Synthetic Production Shape",
                  "type": "xlsx",
                  "filename": "synthetic-production-shape-${timestamp}.xlsx",
                  "xlsx": {
                    "mode": "streaming",
                    "rowAccessWindowSize": %d,
                    "compressTempFiles": true,
                    "useSharedStringsTable": false
                  },
                  "styles": [
                    {
                      "type": "cell",
                      "name": "title",
                      "alignment": "center",
                      "verticalAlignment": "center",
                      "wrapText": true
                    },
                    {
                      "type": "cell",
                      "name": "header",
                      "alignment": "center",
                      "verticalAlignment": "center",
                      "wrapText": true
                    }
                  ],
                  "sheets": [
                %s
                  ]
                }
                """.formatted(reportId, config.rowWindow(), sheets);
    }

    private static String sheetJson(int sheetNumber, long start, long end, long rows, int columns) {
        StringBuilder columnJson = new StringBuilder();
        StringBuilder select = new StringBuilder();
        for (int i = 1; i <= columns; i++) {
            if (i > 1) {
                columnJson.append(",\n");
                select.append(", ");
            }
            columnJson.append("""
                              { "title": "Column %d", "key": "c%d", "width": 12 }""".formatted(i, i));
            select.append("x + ").append(i - 1).append(" as c").append(i);
        }
        int titleEndCol = Math.max(1, columns);
        return """
                    {
                      "name": "%s",
                      "title": "%s",
                      "elements": [
                        {
                          "type": "spannedText",
                          "text": "Synthetic Orion Cycle Detail",
                          "upperLeft": { "row": 1, "col": 1 },
                          "lowerRight": { "row": 1, "col": %d },
                          "height": 24,
                          "style": "title"
                        },
                        {
                          "type": "table",
                          "name": "detail_%02d",
                          "style": "header",
                          "upperLeft": { "row": 3, "col": 1 },
                          "columns": [
                %s
                          ],
                          "dataSource": {
                            "name": "synthetic",
                            "filter": {
                              "type": "query",
                              "query": "select %s from system_range(:rangeStart, :rangeEnd)",
                              "parameters": {
                                "rangeStart": { "value": %d, "jdbcType": "BIGINT" },
                                "rangeEnd": { "value": %d, "jdbcType": "BIGINT" }
                              }
                            }
                          }
                        }
                      ]
                    }""".formatted(sheetName(sheetNumber), sheetName(sheetNumber), titleEndCol, sheetNumber,
                columnJson, select, start, end);
    }

    private static Path artifactPath(Path storeDir, String runId) throws IOException {
        Path runDir = storeDir.resolve("runs").resolve(runId);
        try (var stream = Files.list(runDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".xlsx"))
                    .findFirst()
                    .orElseThrow(() -> new IOException("No XLSX artifact found for run " + runId));
        }
    }

    private static ZipValidation validateZip(Path artifact, int expectedSheets) throws IOException {
        int worksheetEntries = 0;
        long sampledBytes = 0L;
        boolean contentTypes = false;
        boolean workbook = false;
        byte[] buffer = new byte[1024 * 128];
        try (ZipFile zip = new ZipFile(artifact.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if ("[Content_Types].xml".equals(entry.getName())) {
                    contentTypes = true;
                }
                if ("xl/workbook.xml".equals(entry.getName())) {
                    workbook = true;
                }
                if (entry.getName().startsWith("xl/worksheets/sheet") && entry.getName().endsWith(".xml")) {
                    worksheetEntries++;
                }
                try (InputStream input = zip.getInputStream(entry)) {
                    long entryRead = 0L;
                    int read;
                    while ((read = input.read(buffer, 0, sampleLimit(entry, entryRead, buffer.length))) != -1) {
                        entryRead += read;
                        sampledBytes += read;
                        if (isWorksheet(entry) && entryRead >= 4096) {
                            break;
                        }
                    }
                }
            }
        }
        if (!contentTypes || !workbook || worksheetEntries != expectedSheets) {
            throw new IllegalStateException("Invalid XLSX ZIP structure: contentTypes=" + contentTypes
                    + ", workbook=" + workbook + ", sheets=" + worksheetEntries);
        }
        return new ZipValidation(true, worksheetEntries, sampledBytes);
    }

    private static int sampleLimit(ZipEntry entry, long entryRead, int bufferLength) {
        if (!isWorksheet(entry)) {
            return bufferLength;
        }
        long remaining = Math.max(0L, 4096L - entryRead);
        return (int) Math.min(bufferLength, remaining);
    }

    private static boolean isWorksheet(ZipEntry entry) {
        return entry.getName().startsWith("xl/worksheets/sheet") && entry.getName().endsWith(".xml");
    }

    private static int readStyleCount(Path artifact) throws IOException {
        Pattern pattern = Pattern.compile("<cellXfs[^>]*count=\"(\\d+)\"");
        try (ZipFile zip = new ZipFile(artifact.toFile())) {
            ZipEntry entry = zip.getEntry("xl/styles.xml");
            if (entry == null) {
                return -1;
            }
            String styles = new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = pattern.matcher(styles);
            return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
        }
    }

    private static void print(Config config, RunResult result, ValidationResult smallValidation) {
        double seconds = result.generationElapsedMillis() / 1000.0;
        double rowsPerSecond = config.totalRows() / seconds;
        System.out.println("MOG API Production Shape Large Report Validation");
        System.out.println("runId=" + result.runId());
        System.out.println("postResponseMillis=" + result.postResponseMillis());
        System.out.println("statusTransitions=" + result.transitions());
        System.out.println("totalDetailRows=" + WHOLE.format(config.totalRows()));
        System.out.println("sheetCount=" + WHOLE.format(config.sheetCount()));
        System.out.println("rowsPerSheet=" + Shape.even(config.totalRows(), config.sheetCount()).sheetRows());
        System.out.println("columns=" + WHOLE.format(config.columns()));
        System.out.println("rowAccessWindowSize=" + WHOLE.format(config.rowWindow()));
        System.out.println("jdbcFetchSize=" + WHOLE.format(config.fetchSize()));
        System.out.println("generationElapsedSeconds=" + DECIMAL.format(seconds));
        System.out.println("rowsPerSecond=" + WHOLE.format(Math.round(rowsPerSecond)));
        System.out.println("usedHeapBeforeMiBApprox=" + DECIMAL.format(toMiB(result.heapBefore())));
        System.out.println("peakUsedHeapMiBApproxSampled=" + DECIMAL.format(toMiB(result.peakHeap())));
        System.out.println("usedHeapAfterGcMiBApprox=" + DECIMAL.format(toMiB(result.heapAfterGc())));
        System.out.println("sxssfTempBytesBeforeApprox=" + WHOLE.format(result.tempBefore()));
        System.out.println("sxssfTempBytesPeakApproxSampled=" + WHOLE.format(result.peakTemp()));
        System.out.println("sxssfTempBytesAfterCompletionApprox=" + WHOLE.format(result.tempAfterCompletion()));
        System.out.println("xlsxFileSizeBytes=" + WHOLE.format(result.artifactSize()));
        System.out.println("xlsxFileSizeMiB=" + DECIMAL.format(toMiB(result.artifactSize())));
        System.out.println("styleCountFromStylesXml=" + result.styleCount());
        System.out.println("zipValidationPerformed=" + result.zipValidation().performed());
        System.out.println("zipWorksheetEntries=" + result.zipValidation().worksheetEntries());
        System.out.println("zipSampledBytesRead=" + WHOLE.format(result.zipValidation().sampledBytes()));
        System.out.println("artifactGetAfterCompletionStatus=" + result.artifactGetStatus());
        System.out.println("artifactGetContentLength=" + WHOLE.format(result.artifactGetContentLength()));
        System.out.println("artifactGetFirstBytesRead=" + WHOLE.format(result.artifactGetFirstBytesRead()));
        System.out.println("jdbcPrepareStatementCount=" + WHOLE.format(CountingDriverMetrics.prepareStatementCount.get()));
        System.out.println("jdbcExecuteQueryCount=" + WHOLE.format(CountingDriverMetrics.executeQueryCount.get()));
        System.out.println("jdbcResultSetOpenCount=" + WHOLE.format(CountingDriverMetrics.resultSetOpenCount.get()));
        System.out.println("jdbcResultSetCloseCount=" + WHOLE.format(CountingDriverMetrics.resultSetCloseCount.get()));
        System.out.println("jdbcConnectionOpenCount=" + WHOLE.format(CountingDriverMetrics.connectionOpenCount.get()));
        System.out.println("jdbcConnectionCloseCount=" + WHOLE.format(CountingDriverMetrics.connectionCloseCount.get()));
        System.out.println("jdbcMaxConcurrentResultSets=" + WHOLE.format(CountingDriverMetrics.maxActiveResultSets.get()));
        System.out.println("jdbcMaxConcurrentConnections=" + WHOLE.format(CountingDriverMetrics.maxActiveConnections.get()));
        System.out.println("smallWorkbookValidationPerformed=" + smallValidation.performed());
        System.out.println("smallWorkbookStyleCount=" + smallValidation.styleCount());
        System.out.println("smallWorkbookTransitions=" + smallValidation.transitions());
        System.out.println("artifactPath=" + result.artifactPath());
        System.out.println("Note: heap and temp/spool values are approximate developer diagnostics.");
    }

    private static String sheetName(int sheetNumber) {
        return String.format(Locale.ROOT, "Cycle %02d", sheetNumber);
    }

    private static void quietLogging() {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)
                org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.WARN);
    }

    private static void recreateDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (var stream = Files.walk(directory)) {
                for (Path path : stream.sorted((a, b) -> b.compareTo(a)).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
        Files.createDirectories(directory);
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
            return 0L;
        }
    }

    private record Config(long totalRows, int sheetCount, int columns, int rowWindow, int fetchSize,
            int workers, int queueCapacity, boolean deleteOutput, boolean validateZip,
            long smallValidationRows, int smallValidationSheets) {
        static Config parse(String[] args) {
            long totalRows = 14_194_708L;
            int sheetCount = 15;
            int columns = 10;
            int rowWindow = 100;
            int fetchSize = 1000;
            int workers = 1;
            int queueCapacity = 10;
            boolean deleteOutput = true;
            boolean validateZip = true;
            long smallValidationRows = 1000L;
            int smallValidationSheets = 3;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                String value = i + 1 < args.length ? args[++i] : "";
                switch (arg) {
                    case "--totalRows" -> totalRows = Long.parseLong(value);
                    case "--sheetCount" -> sheetCount = Integer.parseInt(value);
                    case "--columns" -> columns = Integer.parseInt(value);
                    case "--rowWindow" -> rowWindow = Integer.parseInt(value);
                    case "--fetchSize" -> fetchSize = Integer.parseInt(value);
                    case "--workers" -> workers = Integer.parseInt(value);
                    case "--queueCapacity" -> queueCapacity = Integer.parseInt(value);
                    case "--deleteOutput" -> deleteOutput = Boolean.parseBoolean(value);
                    case "--validateZip" -> validateZip = Boolean.parseBoolean(value);
                    case "--smallValidationRows" -> smallValidationRows = Long.parseLong(value);
                    case "--smallValidationSheets" -> smallValidationSheets = Integer.parseInt(value);
                    default -> throw new IllegalArgumentException("Unsupported argument: " + arg);
                }
            }
            if (totalRows < 1 || sheetCount < 1 || columns < 1) {
                throw new IllegalArgumentException("totalRows, sheetCount, and columns must be positive");
            }
            long maxRows = Shape.even(totalRows, sheetCount).sheetRows().stream().mapToLong(Long::longValue).max().orElse(0L);
            if (maxRows + 3 > 1_048_576L) {
                throw new IllegalArgumentException("Sheet shape exceeds XLSX row limit including title/header rows");
            }
            return new Config(totalRows, sheetCount, columns, rowWindow, fetchSize, workers, queueCapacity,
                    deleteOutput, validateZip, smallValidationRows, smallValidationSheets);
        }
    }

    private record Shape(List<Long> sheetRows) {
        static Shape even(long totalRows, int sheetCount) {
            long base = totalRows / sheetCount;
            long remainder = totalRows % sheetCount;
            List<Long> rows = new ArrayList<>();
            for (int i = 0; i < sheetCount; i++) {
                rows.add(base + (i < remainder ? 1L : 0L));
            }
            return new Shape(rows);
        }
    }

    private record RunObservation(String runId, long postResponseMillis, List<String> transitions,
            long elapsedMillis) {
    }

    private record RunResult(String runId, long postResponseMillis, List<String> transitions,
            long generationElapsedMillis, Path artifactPath, long artifactSize, long heapBefore,
            long peakHeap, long heapAfterGc, long tempBefore, long peakTemp, long tempAfterCompletion,
            ZipValidation zipValidation, int styleCount, int artifactGetStatus, long artifactGetContentLength,
            long artifactGetFirstBytesRead) {
    }

    private record ValidationResult(boolean performed, int styleCount, List<String> transitions) {
        static ValidationResult skipped() {
            return new ValidationResult(false, -1, List.of());
        }
    }

    private record ZipValidation(boolean performed, int worksheetEntries, long sampledBytes) {
        static ZipValidation skipped() {
            return new ZipValidation(false, -1, -1L);
        }
    }

    public static final class CountingDriver implements Driver {
        private static final Driver H2_DRIVER = new org.h2.Driver();

        static {
            try {
                DriverManager.registerDriver(new CountingDriver());
            } catch (SQLException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            if (!acceptsURL(url)) {
                return null;
            }
            Connection delegate = H2_DRIVER.connect(url.replace("jdbc:mogbench:", "jdbc:"), info);
            CountingDriverMetrics.connectionOpenCount.incrementAndGet();
            CountingDriverMetrics.activeConnections.incrementAndGet();
            CountingDriverMetrics.maxActiveConnections.accumulateAndGet(CountingDriverMetrics.activeConnections.get(), Math::max);
            return proxy(Connection.class, delegate, new ConnectionHandler(delegate));
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith("jdbc:mogbench:");
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return java.util.logging.Logger.getGlobal();
        }
    }

    private static final class ConnectionHandler implements InvocationHandler {
        private final Connection delegate;
        private boolean closed;

        private ConnectionHandler(Connection delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("prepareStatement".equals(method.getName())) {
                Object result = method.invoke(delegate, args);
                if (result instanceof PreparedStatement statement) {
                    CountingDriverMetrics.prepareStatementCount.incrementAndGet();
                    return proxy(PreparedStatement.class, statement, new PreparedStatementHandler(statement));
                }
                return result;
            }
            if ("close".equals(method.getName())) {
                try {
                    return method.invoke(delegate, args);
                } finally {
                    if (!closed) {
                        closed = true;
                        CountingDriverMetrics.connectionCloseCount.incrementAndGet();
                        CountingDriverMetrics.activeConnections.decrementAndGet();
                    }
                }
            }
            return method.invoke(delegate, args);
        }
    }

    private static final class PreparedStatementHandler implements InvocationHandler {
        private final PreparedStatement delegate;

        private PreparedStatementHandler(PreparedStatement delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("executeQuery".equals(method.getName())) {
                ResultSet resultSet = (ResultSet) method.invoke(delegate, args);
                CountingDriverMetrics.executeQueryCount.incrementAndGet();
                CountingDriverMetrics.resultSetOpenCount.incrementAndGet();
                CountingDriverMetrics.activeResultSets.incrementAndGet();
                CountingDriverMetrics.maxActiveResultSets.accumulateAndGet(CountingDriverMetrics.activeResultSets.get(), Math::max);
                return proxy(ResultSet.class, resultSet, new ResultSetHandler(resultSet));
            }
            return method.invoke(delegate, args);
        }
    }

    private static final class ResultSetHandler implements InvocationHandler {
        private final ResultSet delegate;
        private boolean closed;

        private ResultSetHandler(ResultSet delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("close".equals(method.getName())) {
                try {
                    return method.invoke(delegate, args);
                } finally {
                    if (!closed) {
                        closed = true;
                        CountingDriverMetrics.resultSetCloseCount.incrementAndGet();
                        CountingDriverMetrics.activeResultSets.decrementAndGet();
                    }
                }
            }
            return method.invoke(delegate, args);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, T delegate, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
    }

    private static final class CountingDriverMetrics {
        private static final AtomicInteger prepareStatementCount = new AtomicInteger();
        private static final AtomicInteger executeQueryCount = new AtomicInteger();
        private static final AtomicInteger resultSetOpenCount = new AtomicInteger();
        private static final AtomicInteger resultSetCloseCount = new AtomicInteger();
        private static final AtomicInteger activeResultSets = new AtomicInteger();
        private static final AtomicInteger maxActiveResultSets = new AtomicInteger();
        private static final AtomicInteger connectionOpenCount = new AtomicInteger();
        private static final AtomicInteger connectionCloseCount = new AtomicInteger();
        private static final AtomicInteger activeConnections = new AtomicInteger();
        private static final AtomicInteger maxActiveConnections = new AtomicInteger();

        private static void reset() {
            prepareStatementCount.set(0);
            executeQueryCount.set(0);
            resultSetOpenCount.set(0);
            resultSetCloseCount.set(0);
            activeResultSets.set(0);
            maxActiveResultSets.set(0);
            connectionOpenCount.set(0);
            connectionCloseCount.set(0);
            activeConnections.set(0);
            maxActiveConnections.set(0);
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
            thread = new Thread(this, "mog-api-production-shape-sampler");
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
