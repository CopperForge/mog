package org.copperforge.mog.api.web;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.copperforge.mog.api.run.RunMetadata;
import org.copperforge.mog.api.run.RunService;
import org.copperforge.mog.api.storage.DslStorageService;
import org.copperforge.mog.api.storage.DslStorageService.SaveResult;
import org.copperforge.mog.contract.run.RunRequest;
import org.copperforge.mog.contract.run.RunResponse;
import org.copperforge.mog.contract.web.SaveDslResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@Validated
public class ApiController {

    private final DslStorageService storageService;
    private final RunService runService;

    public ApiController(DslStorageService storageService, RunService runService) {
        this.storageService = storageService;
        this.runService = runService;
    }

    @GetMapping("/datasources")
    public List<String> listDatasources() throws IOException {
        return storageService.listDatasourceIds();
    }

    @PostMapping("/datasources")
    public SaveDslResponse createDatasource(@RequestBody JsonNode body) throws IOException {
        SaveResult result = storageService.saveDatasource(requireBody(body));
        return new SaveDslResponse(result.id(), true);
    }

    @GetMapping("/datasources/{id}")
    public JsonNode getDatasource(@PathVariable String id) throws IOException {
        return storageService.loadDatasource(id);
    }

    @GetMapping("/reports")
    public List<String> listReports() throws IOException {
        return storageService.listReportIds();
    }

    @PostMapping("/reports")
    public SaveDslResponse createReport(@RequestBody JsonNode body) throws IOException {
        SaveResult result = storageService.saveReport(requireBody(body));
        return new SaveDslResponse(result.id(), true);
    }

    @GetMapping("/reports/{id}")
    public JsonNode getReport(@PathVariable String id) throws IOException {
        return storageService.loadReport(id);
    }

    @GetMapping("/runs")
    public List<RunService.RunSummary> listRuns(@RequestParam(name = "limit", defaultValue = "50") int limit)
            throws IOException {
        return runService.listRuns(limit);
    }

    @PostMapping("/runs")
    public RunResponse startRun(@Valid @RequestBody RunRequest request) throws IOException {
        RunMetadata metadata = runService.execute(request);
        return new RunResponse(metadata.getRunId(), metadata.getStatus().name());
    }

    @GetMapping("/runs/{runId}")
    public RunMetadata getRun(@PathVariable String runId) throws IOException {
        return runService.loadMetadata(runId);
    }

    @GetMapping("/runs/{runId}/artifact")
    public ResponseEntity<InputStreamResource> downloadArtifact(@PathVariable String runId) throws IOException {
        RunService.RunArtifact artifact = runService.loadArtifact(runId);
        InputStreamResource resource = new InputStreamResource(Files.newInputStream(artifact.path()));
        return ResponseEntity.ok()
                .contentLength(artifact.metadata().getSize())
                .contentType(MediaType.parseMediaType(artifact.metadata().getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + artifact.metadata().getFileName() + "\"")
                .body(resource);
    }

    private JsonNode requireBody(JsonNode body) {
        if (body == null || body.isNull()) {
            throw new IllegalArgumentException("Request body must contain JSON");
        }
        return body;
    }
}
