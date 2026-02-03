package org.copperforge.mog.api.run;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.copperforge.mog.MogException;
import org.copperforge.mog.api.config.MogApiProperties;
import org.copperforge.mog.api.storage.DslStorageService;
import org.copperforge.mog.data.catalog.DataSourcesFile;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.runtime.MogContext;
import org.copperforge.mog.runtime.MogRuntime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RunService {

    private final DslStorageService storage;
    private final MogApiProperties properties;
    private final ObjectMapper objectMapper;

    public RunService(DslStorageService storage, MogApiProperties properties, ObjectMapper objectMapper) {
        this.storage = storage;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public RunMetadata execute(RunRequest request) throws IOException {
        Map<String, Object> params = request.params() != null ? new LinkedHashMap<>(request.params()) : Map.of();
        ArtifactFormat format = ArtifactFormat.from(request.output() != null ? request.output().format() : null);

        String runId = UUID.randomUUID().toString();
        Path runDir = storage.ensureRunDirectory(runId);
        RunMetadata metadata = RunMetadata.starting(runId, request.reportId(), request.datasourceId(), params, format.name());
        writeMetadata(runDir, metadata);

        Path reportPath = storage.requireReport(request.reportId());
        Path datasourcePath = storage.requireDatasource(request.datasourceId());
        MogContext context = buildContext(runDir, datasourcePath);

        try {
            Report report = MogRuntime.loadReportDefinition(reportPath.toString(), context);
            attachDataSources(report, datasourcePath);
            Path artifactPath = runDir.resolve(format.artifactFileName());
            report.setFilename(artifactPath.toString());
            String saved = MogRuntime.generateReport(report, context);
            Path actual = Path.of(saved);
            long size = Files.size(actual);
            String fileName = artifactPath.getFileName().toString();
            metadata.markCompleted(new RunMetadata.ArtifactMetadata(fileName, format.contentType(), size));
            writeMetadata(runDir, metadata);
            return metadata;
        } catch (NoSuchFileException e) {
            metadata.markFailed(e.getMessage());
            writeMetadata(runDir, metadata);
            throw e;
        } catch (MogException e) {
            metadata.markFailed(e.getMessage());
            writeMetadata(runDir, metadata);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            metadata.markFailed(e.getMessage());
            writeMetadata(runDir, metadata);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to execute run " + runId, e);
        }
    }

    public RunMetadata loadMetadata(String runId) throws IOException {
        Path meta = storage.metadataFile(runId);
        if (!Files.exists(meta)) {
            throw new NoSuchFileException("Run '" + runId + "' not found");
        }
        return objectMapper.readValue(meta.toFile(), RunMetadata.class);
    }

    public RunArtifact loadArtifact(String runId) throws IOException {
        RunMetadata metadata = loadMetadata(runId);
        RunMetadata.ArtifactMetadata artifactMetadata = metadata.getArtifact();
        if (artifactMetadata == null || artifactMetadata.getFileName() == null) {
            throw new NoSuchFileException("Run '" + runId + "' has no artifact");
        }
        Path artifactPath = storage.runDirectory(runId).resolve(artifactMetadata.getFileName());
        if (!Files.exists(artifactPath)) {
            throw new NoSuchFileException("Artifact missing for run '" + runId + "'");
        }
        return new RunArtifact(artifactPath, artifactMetadata);
    }

    public List<RunSummary> listRuns(int limit) throws IOException {
        int effectiveLimit = limit > 0 ? Math.min(limit, 500) : 50;
        List<RunSummary> summaries = new ArrayList<>();
        for (String runId : storage.listRunIds()) {
            try {
                RunMetadata metadata = loadMetadata(runId);
                summaries.add(RunSummary.from(metadata));
            } catch (NoSuchFileException ex) {
                // run directory without metadata; skip
            }
        }
        Comparator<RunSummary> comparator = Comparator.comparing(RunSummary::startedAt,
                Comparator.nullsLast(Comparator.naturalOrder()));
        summaries.sort(comparator.reversed());
        if (summaries.size() > effectiveLimit) {
            return List.copyOf(summaries.subList(0, effectiveLimit));
        }
        return List.copyOf(summaries);
    }

    private void writeMetadata(Path runDir, RunMetadata metadata) throws IOException {
        Path metaFile = runDir.resolve("meta.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(metaFile.toFile(), metadata);
    }

    private void attachDataSources(Report report, Path datasourcePath) throws IOException {
        DataSourcesFile dsFile = objectMapper.readValue(datasourcePath.toFile(), DataSourcesFile.class);
        if (dsFile != null && dsFile.getDatasources() != null) {
            report.setDataSources(dsFile.getDatasources());
        }
    }

    private MogContext buildContext(Path runDir, Path datasourcePath) {
        return MogContext.builder()
                .datasourcesPath(datasourcePath != null ? datasourcePath.toAbsolutePath().toString() : null)
                .environment(properties.resolvedEnvironment())
                .workingDirectory(runDir.toAbsolutePath().toString())
                .mogHome(properties.resolvedMogHome())
                .mogEtc(properties.resolvedMogEtc())
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
}
