package org.copperforge.mog.api.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.copperforge.mog.MogException;
import org.copperforge.mog.api.config.MogApiProperties;
import org.copperforge.mog.api.storage.FileSystemDslRepository;
import org.copperforge.mog.api.storage.FileSystemRunRepository;
import org.copperforge.mog.api.storage.FileSystemStorageLayout;
import org.copperforge.mog.contract.run.RunRequest;
import org.copperforge.mog.security.MogAESEncryptorDecryptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

class RunServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void execute_loadsMogfFromResolvedMogHomeForJdbcPasswordDecryption() throws Exception {
        String originalUserHome = System.getProperty("user.home");
        Path isolatedUserHome = tempDir.resolve("isolated-user-home");
        Files.createDirectories(isolatedUserHome);
        System.setProperty("user.home", isolatedUserHome.toString());
        try {
            ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
            MogApiProperties properties = new MogApiProperties();
            properties.setStoreDir(tempDir.resolve("store-jdbc"));
            properties.setMogHome(tempDir.toString());
            properties.setMogEtc(tempDir.resolve("etc").toString());

            String encryptionPassword = "test-api-key";
            String jdbcPassword = "test-db-password";
            Files.writeString(tempDir.resolve(".mog"),
                    "{ \"encryptionPassword\": \"" + encryptionPassword + "\" }");
            String encryptedJdbcPassword = new MogAESEncryptorDecryptor(encryptionPassword).encrypt(jdbcPassword);

            String dbName = "api_mogf_" + java.util.UUID.randomUUID().toString().replace("-", "");
            String jdbcUrl = "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1";
            try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", jdbcPassword);
                    Statement statement = connection.createStatement()) {
                statement.execute("create table messages (message varchar(64))");
                statement.execute("insert into messages values ('JDBC-OK')");
            }

            FileSystemStorageLayout layout = new FileSystemStorageLayout(properties);
            FileSystemDslRepository dslRepository = new FileSystemDslRepository(objectMapper, layout);
            FileSystemRunRepository runRepository = new FileSystemRunRepository(layout, objectMapper);
            RunService runService = new RunService(dslRepository, runRepository, properties, objectMapper);

            dslRepository.saveReport(objectMapper.readTree("""
                    {
                      "id": "jdbc-report",
                      "name": "jdbc-report",
                      "type": "xlsx",
                      "sheets": [
                        {
                          "name": "Sheet1",
                          "title": "Sheet1",
                          "elements": [
                            {
                              "type": "table",
                              "name": "t_messages",
                              "upperLeft": { "row": 1, "col": 1 },
                              "columns": [
                                { "title": "Message", "key": "message" }
                              ],
                              "dataSource": {
                                "name": "jdbc",
                                "filter": { "type": "query", "query": "select message from messages" }
                              }
                            }
                          ]
                        }
                      ]
                    }
                    """));
            dslRepository.saveDatasource(objectMapper.readTree("""
                    {
                      "id": "jdbc-datasource",
                      "datasources": [
                        {
                          "name": "jdbc",
                          "type": "jdbc",
                          "jdbcClass": "org.h2.Driver",
                          "url": "%s",
                          "user": "sa",
                          "password": "%s"
                        }
                      ]
                    }
                    """.formatted(jdbcUrl, encryptedJdbcPassword)));

            RunMetadata metadata = runService.execute(new RunRequest("jdbc-report", "jdbc-datasource", Map.of(),
                    new RunRequest.RunOutput("xlsx")));

            assertEquals(RunStatus.COMPLETED, metadata.getStatus());
            Path artifact = runRepository.runDirectory(metadata.getRunId()).resolve(metadata.getArtifact().getFileName());
            try (XSSFWorkbook workbook = new XSSFWorkbook(artifact.toFile())) {
                assertEquals("Message", workbook.getSheet("Sheet1").getRow(0).getCell(0).getStringCellValue());
                assertEquals("JDBC-OK", workbook.getSheet("Sheet1").getRow(1).getCell(0).getStringCellValue());
            }
            runService.shutdown();
        } finally {
            System.setProperty("user.home", originalUserHome);
        }
    }

    @Test
    void execute_missingMogfUsesEmptyMogfAndNonJdbcReportStillCompletes() throws Exception {
        String originalUserHome = System.getProperty("user.home");
        Path isolatedUserHome = tempDir.resolve("missing-mogf-user-home");
        Path isolatedMogHome = tempDir.resolve("missing-mogf-home");
        Files.createDirectories(isolatedUserHome);
        Files.createDirectories(isolatedMogHome);
        System.setProperty("user.home", isolatedUserHome.toString());
        try {
            TestFixture fixture = fixture(1, 10, isolatedMogHome);
            RunService runService = new RunService(fixture.dslRepository, fixture.runRepository, fixture.properties,
                    fixture.objectMapper);

            RunMetadata metadata = runService.execute(fixture.request());

            assertEquals(RunStatus.COMPLETED, metadata.getStatus());
            assertNotNull(metadata.getArtifact());
            runService.shutdown();
        } finally {
            System.setProperty("user.home", originalUserHome);
        }
    }

    @Test
    void execute_usesRunParamsForInlineDatasource_andKeepsInlineDatasourcePrecedence() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        MogApiProperties properties = new MogApiProperties();
        properties.setStoreDir(tempDir.resolve("store"));
        properties.setMogHome(tempDir.toString());
        properties.setMogEtc(tempDir.resolve("etc").toString());

        FileSystemStorageLayout layout = new FileSystemStorageLayout(properties);
        FileSystemDslRepository dslRepository = new FileSystemDslRepository(objectMapper, layout);
        FileSystemRunRepository runRepository = new FileSystemRunRepository(layout, objectMapper);
        RunService runService = new RunService(dslRepository, runRepository, properties, objectMapper);

        Path inlineJson = tempDir.resolve("inline.json");
        Files.writeString(inlineJson, "{\"data\":[{\"message\":\"INLINE\"}]}");

        Path externalJson = tempDir.resolve("external.json");
        Files.writeString(externalJson, "{\"data\":[{\"message\":\"EXTERNAL\"}]}");

        dslRepository.saveReport(objectMapper.readTree("""
                {
                  "id": "report-inline-wins",
                  "name": "report-inline-wins",
                  "type": "xlsx",
                  "filename": "${projectName}-mappings-${timestamp}.xlsx",
                  "dataSources": [
                    { "name": "shared", "type": "json", "file": "${inline_file}" }
                  ],
                  "sheets": [
                    {
                      "name": "Sheet1",
                      "title": "Sheet1",
                      "elements": [
                        {
                          "type": "table",
                          "name": "t_messages",
                          "title": "Messages",
                          "upperLeft": { "row": 1, "col": 1 },
                          "columns": [
                            { "title": "Message", "key": "message" }
                          ],
                          "dataSource": {
                            "name": "shared",
                            "filter": { "type": "json", "jsonPath": "$.data[*]" }
                          }
                        }
                      ]
                    }
                  ]
                }
                """));

        dslRepository.saveDatasource(objectMapper.readTree("""
                {
                  "id": "datasource-inline-wins",
                  "datasources": [
                    { "name": "shared", "type": "json", "file": "%s" }
                  ]
                }
                """.formatted(escapeForJson(externalJson))));

        RunRequest request = new RunRequest(
                "report-inline-wins",
                "datasource-inline-wins",
                Map.of("inline_file", inlineJson.toString(), "projectName", "Claims Conversion"),
                new RunRequest.RunOutput("xlsx"));

        RunMetadata metadata = runService.execute(request);

        assertNotNull(metadata.getRunId());
        String fileName = metadata.getArtifact().getFileName();
        assertNotNull(fileName);
        org.junit.jupiter.api.Assertions.assertTrue(fileName.startsWith("Claims Conversion-mappings-"));
        org.junit.jupiter.api.Assertions.assertTrue(fileName.endsWith(".xlsx"));
        assertEquals("COMPLETED", metadata.getStatus().name());
        assertEquals(inlineJson.toString(), metadata.getParams().get("inline_file"));

        Path artifact = runRepository.runDirectory(metadata.getRunId()).resolve(metadata.getArtifact().getFileName());
        try (XSSFWorkbook workbook = new XSSFWorkbook(artifact.toFile())) {
            assertEquals("Message", workbook.getSheet("Sheet1").getRow(0).getCell(0).getStringCellValue());
            assertEquals("INLINE", workbook.getSheet("Sheet1").getRow(1).getCell(0).getStringCellValue());
        }
        runService.shutdown();
    }

    @Test
    void submit_returnsPromptlyAndPersistsQueuedMetadataBeforeWorkerCompletes() throws Exception {
        TestFixture fixture = fixture(1, 10);
        BlockingRunService runService = new BlockingRunService(fixture);
        RunRequest request = fixture.request();

        long started = System.nanoTime();
        RunMetadata submitted = runService.submit(request);
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();

        assertTrue(elapsedMillis < 500);
        assertNotNull(submitted.getRunId());
        assertEquals(RunStatus.QUEUED, submitted.getStatus());
        assertNotNull(fixture.runRepository.loadMetadata(submitted.getRunId()));

        assertTrue(runService.awaitStarted());
        RunMetadata running = waitForStatus(fixture.runRepository, submitted.getRunId(), RunStatus.RUNNING);
        assertEquals(RunStatus.RUNNING, running.getStatus());

        runService.release();
        waitForStatus(fixture.runRepository, submitted.getRunId(), RunStatus.COMPLETED);
        runService.shutdown();
    }

    @Test
    void asyncRunTransitionsQueuedRunningCompleted() throws Exception {
        TestFixture fixture = fixture(1, 10);
        BlockingRunService runService = new BlockingRunService(fixture);

        RunMetadata submitted = runService.submit(fixture.request());

        assertEquals(RunStatus.QUEUED, submitted.getStatus());
        assertTrue(runService.awaitStarted());
        assertEquals(RunStatus.RUNNING,
                waitForStatus(fixture.runRepository, submitted.getRunId(), RunStatus.RUNNING).getStatus());

        runService.release();

        RunMetadata completed = waitForStatus(fixture.runRepository, submitted.getRunId(), RunStatus.COMPLETED);
        assertEquals(RunStatus.COMPLETED, completed.getStatus());
        assertNotNull(completed.getArtifact());
        runService.shutdown();
    }

    @Test
    void asyncRunTransitionsQueuedRunningFailed() throws Exception {
        TestFixture fixture = fixture(1, 10);
        BlockingRunService runService = new BlockingRunService(fixture);
        runService.fail = true;

        RunMetadata submitted = runService.submit(fixture.request());
        assertTrue(runService.awaitStarted());
        waitForStatus(fixture.runRepository, submitted.getRunId(), RunStatus.RUNNING);

        runService.release();

        RunMetadata failed = waitForStatus(fixture.runRepository, submitted.getRunId(), RunStatus.FAILED);
        assertEquals(RunStatus.FAILED, failed.getStatus());
        assertTrue(failed.getMessage().contains("planned failure"));
        runService.shutdown();
    }

    @Test
    void queueBoundingRejectsBeforePersistingOrphanQueuedRun() throws Exception {
        TestFixture fixture = fixture(1, 1);
        BlockingRunService runService = new BlockingRunService(fixture);

        RunMetadata first = runService.submit(fixture.request());
        assertTrue(runService.awaitStarted());
        RunMetadata second = runService.submit(fixture.request());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> runService.submit(fixture.request()));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        assertEquals(2, fixture.runRepository.listMetadata().size());
        assertNotNull(first.getRunId());
        assertNotNull(second.getRunId());

        runService.release();
        waitForStatus(fixture.runRepository, first.getRunId(), RunStatus.COMPLETED);
        waitForStatus(fixture.runRepository, second.getRunId(), RunStatus.COMPLETED);
        runService.shutdown();
    }

    @Test
    void concurrentReadsDoNotObservePartialMetadataFiles() throws Exception {
        TestFixture fixture = fixture(1, 10);
        BlockingRunService runService = new BlockingRunService(fixture);
        RunMetadata submitted = runService.submit(fixture.request());

        assertTrue(runService.awaitStarted());
        Path metaFile = fixture.runRepository.runDirectory(submitted.getRunId()).resolve("meta.json");
        for (int i = 0; i < 100; i++) {
            assertNotNull(fixture.runRepository.loadMetadata(submitted.getRunId()).getStatus());
            assertNotNull(fixture.objectMapper.readTree(metaFile.toFile()).get("status"));
        }

        runService.release();
        waitForStatus(fixture.runRepository, submitted.getRunId(), RunStatus.COMPLETED);
        runService.shutdown();
    }

    @Test
    void artifactIsConflictBeforeCompletionAndDownloadableAfterCompletion() throws Exception {
        TestFixture fixture = fixture(1, 10);
        BlockingRunService runService = new BlockingRunService(fixture);
        RunMetadata submitted = runService.submit(fixture.request());

        assertTrue(runService.awaitStarted());
        ResponseStatusException beforeComplete = assertThrows(ResponseStatusException.class,
                () -> runService.loadArtifact(submitted.getRunId()));
        assertEquals(HttpStatus.CONFLICT, beforeComplete.getStatusCode());

        runService.release();
        RunMetadata completed = waitForStatus(fixture.runRepository, submitted.getRunId(), RunStatus.COMPLETED);

        RunService.RunArtifact artifact = runService.loadArtifact(completed.getRunId());
        assertEquals("artifact.xlsx", artifact.metadata().getFileName());
        assertEquals(4L, artifact.metadata().getSize());
        assertEquals("test", Files.readString(artifact.path()));
        runService.shutdown();
    }

    @Test
    void restartReconciliationFailsStaleQueuedAndRunningRunsOnly() throws Exception {
        TestFixture fixture = fixture(1, 10);
        RunMetadata queued = RunMetadata.queued("queued", "report-inline-wins", "datasource-inline-wins", Map.of(), "XLSX");
        RunMetadata running = RunMetadata.queued("running", "report-inline-wins", "datasource-inline-wins", Map.of(), "XLSX");
        running.markRunning();
        RunMetadata completed = RunMetadata.queued("completed", "report-inline-wins", "datasource-inline-wins", Map.of(), "XLSX");
        completed.markCompleted(new RunMetadata.ArtifactMetadata("a.xlsx", "application/test", 1));
        RunMetadata failed = RunMetadata.queued("failed", "report-inline-wins", "datasource-inline-wins", Map.of(), "XLSX");
        failed.markFailed("already failed");
        for (RunMetadata metadata : List.of(queued, running, completed, failed)) {
            fixture.runRepository.saveMetadata(metadata);
        }

        RunService runService = new RunService(fixture.dslRepository, fixture.runRepository, fixture.properties,
                fixture.objectMapper);
        runService.reconcileInterruptedRuns();

        assertEquals(RunStatus.FAILED, fixture.runRepository.loadMetadata("queued").getStatus());
        assertEquals(RunStatus.FAILED, fixture.runRepository.loadMetadata("running").getStatus());
        assertTrue(fixture.runRepository.loadMetadata("queued").getMessage().contains("restart"));
        assertEquals(RunStatus.COMPLETED, fixture.runRepository.loadMetadata("completed").getStatus());
        assertEquals(RunStatus.FAILED, fixture.runRepository.loadMetadata("failed").getStatus());
        assertEquals("already failed", fixture.runRepository.loadMetadata("failed").getMessage());
        runService.shutdown();
    }

    private String escapeForJson(Path path) {
        return path.toString().replace("\\", "\\\\");
    }

    private TestFixture fixture(int workers, int queueCapacity) throws Exception {
        return fixture(workers, queueCapacity, tempDir);
    }

    private TestFixture fixture(int workers, int queueCapacity, Path mogHome) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        MogApiProperties properties = new MogApiProperties();
        properties.setStoreDir(tempDir.resolve("store-" + java.util.UUID.randomUUID()));
        properties.setMogHome(mogHome.toString());
        properties.setMogEtc(tempDir.resolve("etc").toString());
        properties.getRuns().setWorkers(workers);
        properties.getRuns().setQueueCapacity(queueCapacity);

        FileSystemStorageLayout layout = new FileSystemStorageLayout(properties);
        FileSystemDslRepository dslRepository = new FileSystemDslRepository(objectMapper, layout);
        FileSystemRunRepository runRepository = new FileSystemRunRepository(layout, objectMapper);
        writeMinimalDefinitions(objectMapper, dslRepository);
        return new TestFixture(objectMapper, properties, dslRepository, runRepository);
    }

    private void writeMinimalDefinitions(ObjectMapper objectMapper, FileSystemDslRepository dslRepository) throws Exception {
        dslRepository.saveReport(objectMapper.readTree("""
                {
                  "id": "report-inline-wins",
                  "name": "report-inline-wins",
                  "type": "xlsx",
                  "sheets": []
                }
                """));
        dslRepository.saveDatasource(objectMapper.readTree("""
                {
                  "id": "datasource-inline-wins",
                  "datasources": []
                }
                """));
    }

    private RunMetadata waitForStatus(FileSystemRunRepository repository, String runId, RunStatus status)
            throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        RunMetadata metadata = null;
        while (System.nanoTime() < deadline) {
            metadata = repository.loadMetadata(runId);
            if (metadata.getStatus() == status) {
                return metadata;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("Run " + runId + " did not reach " + status
                + "; last status was " + (metadata != null ? metadata.getStatus() : null));
    }

    private record TestFixture(ObjectMapper objectMapper, MogApiProperties properties,
            FileSystemDslRepository dslRepository, FileSystemRunRepository runRepository) {
        RunRequest request() {
            return new RunRequest("report-inline-wins", "datasource-inline-wins", Map.of("projectName", "Async"),
                    new RunRequest.RunOutput("xlsx"));
        }
    }

    private static final class BlockingRunService extends RunService {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private volatile boolean fail;

        private BlockingRunService(TestFixture fixture) {
            super(fixture.dslRepository(), fixture.runRepository(), fixture.properties(), fixture.objectMapper());
        }

        @Override
        protected void executeRun(RunMetadata metadata) throws Exception {
            started.countDown();
            if (!release.await(10, TimeUnit.SECONDS)) {
                throw new MogException("timed out waiting for test release");
            }
            if (fail) {
                throw new MogException("planned failure");
            }
            Path artifact = runDirectory(metadata.getRunId()).resolve("artifact.xlsx");
            Files.writeString(artifact, "test");
            metadata.markCompleted(new RunMetadata.ArtifactMetadata("artifact.xlsx", "application/test", 4));
            saveMetadata(metadata);
        }

        private boolean awaitStarted() throws InterruptedException {
            return started.await(10, TimeUnit.SECONDS);
        }

        private void release() {
            release.countDown();
        }
    }
}
