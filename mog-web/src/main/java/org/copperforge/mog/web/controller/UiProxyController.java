package org.copperforge.mog.web.controller;

import java.util.List;

import org.copperforge.mog.web.client.MogApiClient;
import org.copperforge.mog.web.model.ArtifactDownload;
import org.copperforge.mog.web.model.RunMetadata;
import org.copperforge.mog.web.model.RunSummary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequestMapping("/ui/api")
public class UiProxyController {

    private final MogApiClient client;

    public UiProxyController(MogApiClient client) {
        this.client = client;
    }

    @GetMapping("/datasources")
    public List<String> datasources() {
        return client.listDatasources();
    }

    @GetMapping("/datasources/{id}")
    public JsonNode datasource(@PathVariable String id) {
        return client.getDatasource(id);
    }

    @GetMapping("/reports")
    public List<String> reports() {
        return client.listReports();
    }

    @GetMapping("/reports/{id}")
    public JsonNode report(@PathVariable String id) {
        return client.getReport(id);
    }

    @GetMapping("/runs")
    public List<RunSummary> runs(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        return client.listRuns(limit);
    }

    @GetMapping("/runs/{id}")
    public RunMetadata run(@PathVariable String id) {
        return client.getRun(id);
    }

    @GetMapping("/runs/{id}/artifact")
    public ResponseEntity<byte[]> artifact(@PathVariable String id) {
        ArtifactDownload download = client.downloadArtifact(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.filename() + "\"")
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.data());
    }
}
