package org.copperforge.mog.web.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.copperforge.mog.web.client.MogApiClient;
import org.copperforge.mog.web.model.ArtifactDownload;
import org.copperforge.mog.web.model.RunSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UiProxyController.class)
class UiProxyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MogApiClient client;

    @Test
    void runs_passthroughsJsonList() throws Exception {
        when(client.listRuns(10)).thenReturn(List.of(new RunSummary("run-1", "COMPLETED",
                Instant.parse("2026-03-28T10:15:30Z"), Instant.parse("2026-03-28T10:16:00Z"), "artifact.xlsx")));

        mockMvc.perform(get("/ui/api/runs").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].runId").value("run-1"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void artifact_returnsBinaryResponse() throws Exception {
        when(client.downloadArtifact("run-123"))
                .thenReturn(new ArtifactDownload("binary".getBytes(), "artifact.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        mockMvc.perform(get("/ui/api/runs/run-123/artifact"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"artifact.xlsx\""))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes("binary".getBytes()));
    }
}
