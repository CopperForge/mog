package org.copperforge.mog.api.run;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.copperforge.mog.MogException;
import org.copperforge.mog.api.config.MogApiProperties;
import org.copperforge.mog.api.storage.DslRepository;
import org.copperforge.mog.api.storage.RunRepository;
import org.copperforge.mog.contract.run.RunRequest;
import org.copperforge.mog.data.MogDataSource;
import org.copperforge.mog.data.catalog.DataSourcesFile;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.runtime.MogContext;
import org.copperforge.mog.runtime.MogRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class RunService {

    private static final String RESTART_FAILURE_MESSAGE = "Run interrupted by service restart before completion";

    private final Logger log = LoggerFactory.getLogger(RunService.class);

    private final DslRepository dslRepository;
    private final RunRepository runRepository;
    private final MogApiProperties properties;
    private final ObjectMapper objectMapper;
    private final ThreadPoolExecutor executor;
    private final Semaphore runSlots;

    public RunService(DslRepository dslRepository, RunRepository runRepository,
            MogApiProperties properties, ObjectMapper objectMapper) {
        this.dslRepository = dslRepository;
        this.runRepository = runRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;

        int workers = Math.max(1, properties.getRuns().getWorkers());
        int queueCapacity = Math.max(0, properties.getRuns().getQueueCapacity());
        BlockingQueue<Runnable> queue = queueCapacity > 0
                ? new ArrayBlockingQueue<>(queueCapacity)
                : new SynchronousQueue<>();
        this.executor = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
                queue, new RunThreadFactory());
        this.runSlots = new Semaphore(workers + queueCapacity);
    }

    @PostConstruct
    public void reconcileInterruptedRuns() throws IOException {
        for (RunMetadata metadata : runRepository.listMetadata()) {
            if (isActiveStatus(metadata.getStatus())) {
                metadata.markFailed(RESTART_FAILURE_MESSAGE);
                runRepository.saveMetadata(metadata);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            int timeout = Math.max(0, properties.getRuns().getShutdownTimeoutSeconds());
            if (!executor.awaitTermination(timeout, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    public RunMetadata submit(RunRequest request) throws IOException {
        PreparedRun prepared = prepare(request);
        if (!runSlots.tryAcquire()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Run queue is full; try again later");
        }

        RunMetadata metadata = createRun(prepared);
        try {
            executor.execute(() -> executeQueuedRun(metadata.getRunId()));
            return metadata;
        } catch (RejectedExecutionException e) {
            runSlots.release();
            metadata.markFailed("Run rejected because the executor is not accepting work");
            runRepository.saveMetadata(metadata);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Run executor is not accepting work", e);
        }
    }

    public RunMetadata execute(RunRequest request) throws IOException {
        PreparedRun prepared = prepare(request);
        RunMetadata metadata = createRun(prepared);
        executeQueuedRun(metadata.getRunId(), false);
        return loadMetadata(metadata.getRunId());
    }

    public void executeQueuedRun(String runId) {
        executeQueuedRun(runId, true);
    }

    private void executeQueuedRun(String runId, boolean releaseSlot) {
        try {
            RunMetadata metadata = runRepository.loadMetadata(runId);
            if (!isActiveStatus(metadata.getStatus())) {
                return;
            }
            metadata.markRunning();
            runRepository.saveMetadata(metadata);
            executeRun(metadata);
        } catch (Exception e) {
            failRun(runId, e);
        } finally {
            if (releaseSlot) {
                runSlots.release();
            }
        }
    }

    protected void executeRun(RunMetadata metadata) throws Exception {
        Path runDir = runRepository.runDirectory(metadata.getRunId());
        Path reportPath = dslRepository.requireReport(metadata.getReportId());
        Path datasourcePath = dslRepository.requireDatasource(metadata.getDatasourceId());
        MogContext context = buildContext(runDir, datasourcePath, metadata.getParams());
        ArtifactFormat format = ArtifactFormat.from(metadata.getFormat());

        Report report = MogRuntime.loadReportDefinition(reportPath.toString(), context);
        attachDataSources(report, datasourcePath);
        Path artifactPath = resolveArtifactPath(runDir, report, context, format);
        report.setFilename(artifactPath.toString());
        String saved = MogRuntime.generateReport(report, context);
        Path actual = Path.of(saved);
        long size = java.nio.file.Files.size(actual);
        String fileName = actual.getFileName().toString();
        metadata.markCompleted(new RunMetadata.ArtifactMetadata(fileName, format.contentType(), size));
        saveMetadata(metadata);
    }

    protected void saveMetadata(RunMetadata metadata) throws IOException {
        runRepository.saveMetadata(metadata);
    }

    protected Path runDirectory(String runId) {
        return runRepository.runDirectory(runId);
    }

    public RunMetadata loadMetadata(String runId) throws IOException {
        return runRepository.loadMetadata(runId);
    }

    public RunArtifact loadArtifact(String runId) throws IOException {
        RunMetadata metadata = loadMetadata(runId);
        if (metadata.getStatus() != RunStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Run '" + runId + "' has not completed");
        }
        RunMetadata.ArtifactMetadata artifactMetadata = metadata.getArtifact();
        if (artifactMetadata == null || artifactMetadata.getFileName() == null) {
            throw new NoSuchFileException("Run '" + runId + "' has no artifact");
        }
        Path artifactPath = runRepository.runDirectory(runId).resolve(artifactMetadata.getFileName());
        if (!java.nio.file.Files.exists(artifactPath)) {
            throw new NoSuchFileException("Artifact missing for run '" + runId + "'");
        }
        return new RunArtifact(artifactPath, artifactMetadata);
    }

    public List<RunSummary> listRuns(int limit) throws IOException {
        int effectiveLimit = limit > 0 ? Math.min(limit, 500) : 50;
        List<RunSummary> summaries = runRepository.listMetadata().stream()
                .map(RunSummary::from)
                .sorted(Comparator.comparing(RunSummary::startedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
        if (summaries.size() > effectiveLimit) {
            return List.copyOf(summaries.subList(0, effectiveLimit));
        }
        return List.copyOf(summaries);
    }

    private PreparedRun prepare(RunRequest request) throws IOException {
        if (request == null) {
            throw new IllegalArgumentException("Run request is required");
        }
        if (request.reportId() == null || request.reportId().isBlank()) {
            throw new IllegalArgumentException("reportId is required");
        }
        if (request.datasourceId() == null || request.datasourceId().isBlank()) {
            throw new IllegalArgumentException("datasourceId is required");
        }
        Map<String, Object> params = request.params() != null ? new LinkedHashMap<>(request.params()) : Map.of();
        ArtifactFormat format = ArtifactFormat.from(request.output() != null ? request.output().format() : null);
        Path reportPath = dslRepository.requireReport(request.reportId());
        Path datasourcePath = dslRepository.requireDatasource(request.datasourceId());
        validateDefinitions(reportPath, datasourcePath, params);
        return new PreparedRun(request.reportId(), request.datasourceId(), params, format);
    }

    private RunMetadata createRun(PreparedRun prepared) throws IOException {
        String runId = UUID.randomUUID().toString();
        runRepository.createRunDirectory(runId);
        RunMetadata metadata = RunMetadata.queued(runId, prepared.reportId(), prepared.datasourceId(),
                prepared.params(), prepared.format().name());
        runRepository.saveMetadata(metadata);
        return metadata;
    }

    private void validateDefinitions(Path reportPath, Path datasourcePath, Map<String, Object> params)
            throws IOException {
        try {
            MogContext context = buildContext(Path.of("."), datasourcePath, params);
            MogRuntime.loadReportDefinition(reportPath.toString(), context);
            objectMapper.readValue(datasourcePath.toFile(), DataSourcesFile.class);
        } catch (MogException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid run definition", e);
        }
    }

    private void failRun(String runId, Exception failure) {
        RunMetadata metadata = null;
        try {
            metadata = runRepository.loadMetadata(runId);
            metadata.markFailed(failure.getMessage() != null ? failure.getMessage() : failure.getClass().getName());
            runRepository.saveMetadata(metadata);
        } catch (Exception saveFailure) {
            log.error("Unable to persist failed run metadata for runId={}", runId, saveFailure);
        }
        String reportId = metadata != null ? metadata.getReportId() : "<unknown>";
        log.error("MOG API run failed runId={} reportId={}", runId, reportId, failure);
    }

    private boolean isActiveStatus(RunStatus status) {
        return status == RunStatus.QUEUED || status == RunStatus.RUNNING || status == RunStatus.STARTED;
    }

    private void attachDataSources(Report report, Path datasourcePath) throws IOException {
        DataSourcesFile dsFile = objectMapper.readValue(datasourcePath.toFile(), DataSourcesFile.class);
        List<MogDataSource> merged = new ArrayList<>();
        Map<String, MogDataSource> named = new LinkedHashMap<>();
        LinkedHashSet<MogDataSource> unnamed = new LinkedHashSet<>();

        if (dsFile != null && dsFile.getDatasources() != null) {
            mergeDataSources(dsFile.getDatasources(), named, unnamed);
        }
        if (report != null && report.getDataSources() != null) {
            mergeDataSources(report.getDataSources(), named, unnamed);
        }

        merged.addAll(unnamed);
        merged.addAll(named.values());
        report.setDataSources(merged);
    }

    private Path resolveArtifactPath(Path runDir, Report report, MogContext context, ArtifactFormat format)
            throws MogException {
        if (report != null && report.getFilename() != null && !report.getFilename().isBlank()) {
            String resolved = org.copperforge.mog.io.MogFileNameBuilder.build(report.getFilename(), context);
            if (resolved != null && !resolved.isBlank()) {
                Path candidate = Path.of(resolved).getFileName();
                if (candidate != null) {
                    String fileName = candidate.toString();
                    if (!fileName.isBlank()) {
                        return runDir.resolve(fileName);
                    }
                }
            }
        }
        return runDir.resolve(format.artifactFileName());
    }

    private void mergeDataSources(List<MogDataSource> sources, Map<String, MogDataSource> named,
            LinkedHashSet<MogDataSource> unnamed) {
        for (MogDataSource dataSource : sources) {
            if (dataSource == null) {
                continue;
            }
            if (dataSource.getName() == null || dataSource.getName().isBlank()) {
                unnamed.add(dataSource);
            } else {
                named.put(dataSource.getName(), dataSource);
            }
        }
    }

    private MogContext buildContext(Path runDir, Path datasourcePath, Map<String, Object> params) {
        return MogContext.builder()
                .datasourcesPath(datasourcePath != null ? datasourcePath.toAbsolutePath().toString() : null)
                .environment(properties.resolvedEnvironment())
                .workingDirectory(runDir.toAbsolutePath().toString())
                .mogHome(properties.resolvedMogHome())
                .mogEtc(properties.resolvedMogEtc())
                .variables(params)
                .build();
    }

    public record RunArtifact(Path path, RunMetadata.ArtifactMetadata metadata) { }

    public record RunSummary(String runId, RunStatus status, java.time.Instant startedAt,
            java.time.Instant finishedAt, String artifactName) {
        static RunSummary from(RunMetadata metadata) {
            String artifact = metadata.getArtifact() != null ? metadata.getArtifact().getFileName() : null;
            return new RunSummary(metadata.getRunId(), metadata.getStatus(), metadata.getStartedAt(),
                    metadata.getCompletedAt(), artifact);
        }
    }

    private record PreparedRun(String reportId, String datasourceId, Map<String, Object> params,
            ArtifactFormat format) {
    }

    private static final class RunThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "mog-api-run-worker-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
