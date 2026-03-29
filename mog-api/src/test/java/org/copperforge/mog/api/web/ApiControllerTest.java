package org.copperforge.mog.api.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;

import org.copperforge.mog.api.run.RunMetadata;
import org.copperforge.mog.api.run.RunService;
import org.copperforge.mog.api.run.RunStatus;
import org.copperforge.mog.api.storage.DslRepository;
import org.copperforge.mog.contract.run.RunRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(ApiController.class)
@Import(ApiExceptionHandler.class)
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DslRepository dslRepository;

    @MockBean
    private RunService runService;

    @TempDir
    Path tempDir;

    @Test
    void createDatasource_returnsSavedId() throws Exception {
        when(dslRepository.saveDatasource(any(JsonNode.class)))
                .thenReturn(new DslRepository.SaveResult("shared", tempDir.resolve("shared.json")));

        mockMvc.perform(post("/api/datasources")
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":\"shared\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("shared"))
                .andExpect(jsonPath("$.saved").value(true));
    }

    @Test
    void getReport_returnsStoredJson() throws Exception {
        when(dslRepository.loadReport("sales")).thenReturn(objectMapper.readTree("{\"id\":\"sales\",\"name\":\"Sales\"}"));

        mockMvc.perform(get("/api/reports/sales"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":\"sales\",\"name\":\"Sales\"}"));
    }

    @Test
    void startRun_passesParamsAndReturnsRunResponse() throws Exception {
        RunMetadata metadata = RunMetadata.starting("run-123", "sales", "shared", Map.of("year", 2025), "XLSX");
        metadata.setStatus(RunStatus.STARTED);
        when(runService.execute(any(RunRequest.class))).thenReturn(metadata);

        mockMvc.perform(post("/api/runs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reportId": "sales",
                                  "datasourceId": "shared",
                                  "params": { "year": 2025 },
                                  "output": { "format": "XLSX" }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("run-123"))
                .andExpect(jsonPath("$.status").value("STARTED"));

        ArgumentCaptor<RunRequest> requestCaptor = ArgumentCaptor.forClass(RunRequest.class);
        verify(runService).execute(requestCaptor.capture());
        RunRequest request = requestCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("sales", request.reportId());
        org.junit.jupiter.api.Assertions.assertEquals("shared", request.datasourceId());
        org.junit.jupiter.api.Assertions.assertEquals(2025, request.params().get("year"));
        org.junit.jupiter.api.Assertions.assertEquals("XLSX", request.output().format());
    }

    @Test
    void startRun_badRequestIsSurfaced() throws Exception {
        when(runService.execute(any(RunRequest.class)))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Broken report"));

        mockMvc.perform(post("/api/runs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reportId": "sales",
                                  "datasourceId": "shared",
                                  "output": { "format": "XLSX" }
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startRun_validationFailureReturnsDetails() throws Exception {
        mockMvc.perform(post("/api/runs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reportId": "",
                                  "datasourceId": "",
                                  "output": { "format": "XLSX" }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void downloadArtifact_returnsBinaryPayload() throws Exception {
        Path artifactPath = tempDir.resolve("artifact.xlsx");
        java.nio.file.Files.writeString(artifactPath, "xlsx-bytes");
        RunMetadata.ArtifactMetadata artifact = new RunMetadata.ArtifactMetadata("artifact.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 10);
        when(runService.loadArtifact("run-123")).thenReturn(new RunService.RunArtifact(artifactPath, artifact));

        mockMvc.perform(get("/api/runs/run-123/artifact"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"artifact.xlsx\""))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(java.nio.file.Files.readAllBytes(artifactPath)));
    }

    @Test
    void missingRunReturnsNotFound() throws Exception {
        when(runService.loadMetadata(eq("missing"))).thenThrow(new NoSuchFileException("Run 'missing' not found"));

        mockMvc.perform(get("/api/runs/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Run 'missing' not found"));
    }
}
