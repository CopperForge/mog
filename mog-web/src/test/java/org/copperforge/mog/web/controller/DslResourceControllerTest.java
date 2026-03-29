package org.copperforge.mog.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.copperforge.mog.contract.web.SaveDslResponse;
import org.copperforge.mog.web.model.DslResourceType;
import org.copperforge.mog.web.service.DslResourcePageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DslResourceController.class)
class DslResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DslResourcePageService pageService;

    @Test
    void datasourcesPage_usesGenericTemplate() throws Exception {
        when(pageService.listIds(DslResourceType.DATASOURCE)).thenReturn(List.of("shared"));

        mockMvc.perform(get("/ui/datasources"))
                .andExpect(status().isOk())
                .andExpect(view().name("ui/dsl-resource"))
                .andExpect(model().attribute("activePage", "datasources"))
                .andExpect(model().attribute("resourceBasePath", "/ui/datasources"))
                .andExpect(model().attribute("resourceIds", List.of("shared")))
                .andExpect(model().attribute("resourceType", DslResourceType.DATASOURCE));
    }

    @Test
    void reportDetail_usesSharedPageModel() throws Exception {
        when(pageService.listIds(DslResourceType.REPORT)).thenReturn(List.of("sales"));
        when(pageService.loadPrettyJson(DslResourceType.REPORT, "sales")).thenReturn("{\n  \"id\" : \"sales\"\n}");

        mockMvc.perform(get("/ui/reports/sales"))
                .andExpect(status().isOk())
                .andExpect(view().name("ui/dsl-resource"))
                .andExpect(model().attribute("activePage", "reports"))
                .andExpect(model().attribute("selectedId", "sales"))
                .andExpect(model().attribute("selectedJson", "{\n  \"id\" : \"sales\"\n}"))
                .andExpect(model().attribute("resourceType", DslResourceType.REPORT));
    }

    @Test
    void datasourceUpload_redirectsToSavedResource() throws Exception {
        when(pageService.save(eq(DslResourceType.DATASOURCE), any())).thenReturn(new SaveDslResponse("shared", true));

        mockMvc.perform(post("/ui/datasources").param("jsonText", "{\"id\":\"shared\"}"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/datasources/shared"))
                .andExpect(flash().attribute("successMessage", "Datasource shared saved"));

        verify(pageService).save(eq(DslResourceType.DATASOURCE), any());
    }
}
