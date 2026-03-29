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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RunService {

    private final DslRepository dslRepository;
    private final RunRepository runRepository;
    private final MogApiProperties properties;
    private final ObjectMapper objectMapper;

    public RunService(DslRepository dslRepository, RunRepository runRepository,
            MogApiProperties properties, ObjectMapper objectMapper) {
        this.dslRepository = dslRepository;
        this.runRepository = runRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public RunMetadata execute(RunRequest request) throws IOException {
        Map<String, Object> params = request.params() != null ? new LinkedHashMap<>(request.params()) : Map.of();
        ArtifactFormat format = ArtifactFormat.from(request.output() != null ? request.output().format() : null);

        String runId = UUID.randomUUID().toString();
        Path runDir = runRepository.createRunDirectory(runId);
        RunMetadata metadata = RunMetadata.starting(runId, request.reportId(), request.datasourceId(), params, format.name());
        runRepository.saveMetadata(metadata);

        Path reportPath = dslRepository.requireReport(request.reportId());
        Path datasourcePath = dslRepository.requireDatasource(request.datasourceId());
        MogContext context = buildContext(runDir, datasourcePath, params);

        try {
            Report report = MogRuntime.loadReportDefinition(reportPath.toString(), context);
            attachDataSources(report, datasourcePath);
            Path artifactPath = runDir.resolve(format.artifactFileName());
            report.setFilename(artifactPath.toString());
            String saved = MogRuntime.generateReport(report, context);
            Path actual = Path.of(saved);
            long size = java.nio.file.Files.size(actual);
            String fileName = artifactPath.getFileName().toString();
            metadata.markCompleted(new RunMetadata.ArtifactMetadata(fileName, format.contentType(), size));
            runRepository.saveMetadata(metadata);
            return metadata;
        } catch (NoSuchFileException e) {
            metadata.markFailed(e.getMessage());
            runRepository.saveMetadata(metadata);
            throw e;
        } catch (MogException e) {
            metadata.markFailed(e.getMessage());
            runRepository.saveMetadata(metadata);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            metadata.markFailed(e.getMessage());
            runRepository.saveMetadata(metadata);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to execute run " + runId, e);
        }
    }

    public RunMetadata loadMetadata(String runId) throws IOException {
        return runRepository.loadMetadata(runId);
    }

    public RunArtifact loadArtifact(String runId) throws IOException {
        RunMetadata metadata = loadMetadata(runId);
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
}
