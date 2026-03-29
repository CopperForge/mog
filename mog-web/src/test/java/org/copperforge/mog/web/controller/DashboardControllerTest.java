package org.copperforge.mog.web.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.util.List;

import org.copperforge.mog.web.client.MogApiClient;
import org.copperforge.mog.web.config.MogApiClientProperties;
import org.copperforge.mog.web.model.RunSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MogApiClient client;

    @MockBean
    private MogApiClientProperties properties;

    @Test
    void root_redirectsToUi() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui"));
    }

    @Test
    void favicon_redirectsToBrandImage() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/img/mog.png"));
    }

    @Test
    void dashboard_populatesCountsAndRuns() throws Exception {
        when(client.listDatasources()).thenReturn(List.of("shared", "archive"));
        when(client.listReports()).thenReturn(List.of("sales"));
        when(client.listRuns(5)).thenReturn(List.of(new RunSummary("run-1", "COMPLETED",
                Instant.parse("2026-03-28T10:15:30Z"), Instant.parse("2026-03-28T10:16:00Z"), "artifact.xlsx")));

        mockMvc.perform(get("/ui"))
                .andExpect(status().isOk())
                .andExpect(view().name("ui/dashboard"))
                .andExpect(model().attribute("datasourceCount", 2))
                .andExpect(model().attribute("reportCount", 1))
                .andExpect(model().attribute("runCount", 1))
                .andExpect(model().attributeExists("callout"))
                .andExpect(model().attribute("activePage", "dashboard"));
    }

    @Test
    void swagger_usesConfiguredBaseUrl() throws Exception {
        when(properties.getBaseUrl()).thenReturn("http://example.test:8080");

        mockMvc.perform(get("/ui/swagger"))
                .andExpect(status().isOk())
                .andExpect(view().name("ui/swagger"))
                .andExpect(model().attribute("swaggerUrl", "http://example.test:8080/swagger-ui.html"))
                .andExpect(model().attribute("activePage", "swagger"));
    }
}
