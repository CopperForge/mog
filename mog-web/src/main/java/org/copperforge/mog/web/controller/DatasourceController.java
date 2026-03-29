package org.copperforge.mog.web.controller;

import java.util.List;

import org.copperforge.mog.contract.web.SaveDslResponse;
import org.copperforge.mog.web.client.MogApiClient;
import org.copperforge.mog.web.model.DslUploadForm;
import org.copperforge.mog.web.support.JsonHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class DatasourceController {

    private final MogApiClient client;
    private final JsonHelper jsonHelper;
    private final ObjectMapper objectMapper;

    public DatasourceController(MogApiClient client, JsonHelper jsonHelper, ObjectMapper objectMapper) {
        this.client = client;
        this.jsonHelper = jsonHelper;
        this.objectMapper = objectMapper;
    }

    @ModelAttribute("form")
    public DslUploadForm form() {
        return new DslUploadForm();
    }

    @GetMapping("/ui/datasources")
    public String datasources(Model model) {
        populateList(model);
        return "ui/datasources";
    }

    @PostMapping("/ui/datasources")
    public String upload(@ModelAttribute("form") DslUploadForm form, RedirectAttributes redirectAttributes) {
        SaveDslResponse response = client.saveDatasource(jsonHelper.parseBody(form.getJsonText(), form.getFile()));
        redirectAttributes.addFlashAttribute("successMessage", "Datasource " + response.id() + " saved");
        redirectAttributes.addAttribute("id", response.id());
        return "redirect:/ui/datasources/{id}";
    }

    @GetMapping("/ui/datasources/{id}")
    public String viewDatasource(@PathVariable String id, Model model)
            throws Exception {
        populateList(model);
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(client.getDatasource(id));
        model.addAttribute("selectedDatasource", json);
        model.addAttribute("selectedId", id);
        return "ui/datasources";
    }

    private void populateList(Model model) {
        List<String> ids = client.listDatasources();
        model.addAttribute("datasourceIds", ids);
        model.addAttribute("activePage", "datasources");
    }
}
