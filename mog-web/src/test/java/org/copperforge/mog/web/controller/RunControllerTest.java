package org.copperforge.mog.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.util.List;

import org.copperforge.mog.contract.run.RunRequest;
import org.copperforge.mog.contract.run.RunResponse;
import org.copperforge.mog.web.client.MogApiClient;
import org.copperforge.mog.web.model.RunMetadata;
import org.copperforge.mog.web.model.RunSummary;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RunController.class)
class RunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MogApiClient client;

    @Test
    void runsPage_populatesAvailableSelections() throws Exception {
        when(client.listReports()).thenReturn(List.of("sales"));
        when(client.listDatasources()).thenReturn(List.of("shared"));
        when(client.listRuns(50)).thenReturn(List.of(new RunSummary("run-1", "COMPLETED", Instant.parse("2026-03-28T10:15:30Z"),
                Instant.parse("2026-03-28T10:16:00Z"), "artifact.xlsx")));

        mockMvc.perform(get("/ui/runs"))
                .andExpect(status().isOk())
                .andExpect(view().name("ui/runs"))
                .andExpect(model().attribute("reports", List.of("sales")))
                .andExpect(model().attribute("datasources", List.of("shared")))
                .andExpect(model().attributeExists("runHistory"))
                .andExpect(model().attribute("activePage", "runs"));
    }

    @Test
    void startRun_parsesParamsAndRedirectsToRunDetail() throws Exception {
        when(client.runReport(any(RunRequest.class))).thenReturn(new RunResponse("run-123", "STARTED"));

        mockLists();

        mockMvc.perform(post("/ui/runs")
                        .param("reportId", "sales")
                        .param("datasourceId", "shared")
                        .param("paramsJson", "{\"year\":2025}")
                        .param("outputFormat", "csv"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/runs/run-123"))
                .andExpect(flash().attribute("successMessage", "Run run-123 started"));

        ArgumentCaptor<RunRequest> requestCaptor = ArgumentCaptor.forClass(RunRequest.class);
        verify(client).runReport(requestCaptor.capture());
        RunRequest request = requestCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("sales", request.reportId());
        org.junit.jupiter.api.Assertions.assertEquals("shared", request.datasourceId());
        org.junit.jupiter.api.Assertions.assertEquals(2025, request.params().get("year"));
        org.junit.jupiter.api.Assertions.assertEquals("XLSX", request.output().format());
    }

    @Test
    void invalidParamsJson_staysOnRunsPage() throws Exception {
        mockLists();

        mockMvc.perform(post("/ui/runs")
                        .param("reportId", "sales")
                        .param("datasourceId", "shared")
                        .param("paramsJson", "{not-json"))
                .andExpect(status().isOk())
                .andExpect(view().name("ui/runs"))
                .andExpect(model().attributeHasFieldErrors("runForm", "paramsJson"))
                .andExpect(model().attribute("activePage", "runs"));

        verify(client, never()).runReport(any(RunRequest.class));
    }

    @Test
    void runDetail_loadsSelectedRun() throws Exception {
        mockLists();
        RunMetadata metadata = new RunMetadata();
        metadata.setRunId("run-123");
        metadata.setStatus("COMPLETED");
        when(client.getRun("run-123")).thenReturn(metadata);

        mockMvc.perform(get("/ui/runs/run-123"))
                .andExpect(status().isOk())
                .andExpect(view().name("ui/runs"))
                .andExpect(model().attributeExists("selectedRun"))
                .andExpect(model().attribute("activePage", "runs"));
    }

    private void mockLists() {
        when(client.listReports()).thenReturn(List.of("sales"));
        when(client.listDatasources()).thenReturn(List.of("shared"));
        when(client.listRuns(50)).thenReturn(List.of());
    }
}
