package org.copperforge.mog.web.controller;

import java.util.List;
import java.util.Map;

import org.copperforge.mog.web.client.MogApiClient;
import org.copperforge.mog.web.model.RunForm;
import org.copperforge.mog.web.model.RunMetadata;
import org.copperforge.mog.web.model.RunRequestPayload;
import org.copperforge.mog.web.model.RunResponse;
import org.copperforge.mog.web.model.RunSummary;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;

@Controller
public class RunController {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final MogApiClient client;
    private final ObjectMapper objectMapper;

    public RunController(MogApiClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @ModelAttribute("runForm")
    public RunForm form() {
        return new RunForm();
    }

    @GetMapping("/ui/runs")
    public String runs(Model model) {
        populate(model);
        return "ui/runs";
    }

    @PostMapping("/ui/runs")
    public String startRun(@Valid @ModelAttribute("runForm") RunForm form, BindingResult bindingResult,
            Model model, RedirectAttributes redirectAttributes) {
        Map<String, Object> params = Map.of();
        if (form.getParamsJson() != null && !form.getParamsJson().isBlank()) {
            try {
                params = objectMapper.readValue(form.getParamsJson(), MAP_TYPE);
            } catch (Exception ex) {
                bindingResult.rejectValue("paramsJson", "invalid", "Params JSON is invalid");
            }
        }

        if (bindingResult.hasErrors()) {
            populate(model);
            return "ui/runs";
        }

        String format = form.getOutputFormat() != null ? form.getOutputFormat().toUpperCase() : "XLSX";
        if (!format.equals("XLSX")) {
            format = "XLSX";
        }

        RunRequestPayload payload = new RunRequestPayload(form.getReportId(), form.getDatasourceId(), params,
                new RunRequestPayload.Output(format));
        RunResponse response = client.runReport(payload);
        redirectAttributes.addFlashAttribute("successMessage", "Run " + response.runId() + " started");
        return "redirect:/ui/runs/" + response.runId();
    }

    @GetMapping("/ui/runs/{runId}")
    public String runDetail(@PathVariable String runId, Model model) {
        RunMetadata metadata = client.getRun(runId);
        model.addAttribute("selectedRun", metadata);
        populate(model);
        return "ui/runs";
    }

    private void populate(Model model) {
        List<String> reports = client.listReports();
        List<String> datasources = client.listDatasources();
        List<RunSummary> runs = client.listRuns(50);
        model.addAttribute("reports", reports);
        model.addAttribute("datasources", datasources);
        model.addAttribute("runHistory", runs);
        model.addAttribute("activePage", "runs");
    }
}
