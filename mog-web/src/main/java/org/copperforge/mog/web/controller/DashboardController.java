package org.copperforge.mog.web.controller;

import java.util.List;

import org.copperforge.mog.web.client.MogApiClient;
import org.copperforge.mog.web.config.MogApiClientProperties;
import org.copperforge.mog.web.model.RunSummary;
import org.copperforge.mog.web.support.MogMessages;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final MogApiClient client;
    private final MogApiClientProperties properties;

    public DashboardController(MogApiClient client, MogApiClientProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/ui";
    }

    @GetMapping("/favicon.ico")
    public String favicon() {
        return "redirect:/img/mog.png";
    }

    @GetMapping({ "/ui", "/ui/" })
    public String dashboard(Model model) {
        List<String> datasources = client.listDatasources();
        List<String> reports = client.listReports();
        List<RunSummary> runs = client.listRuns(5);

        model.addAttribute("datasourceCount", datasources.size());
        model.addAttribute("reportCount", reports.size());
        model.addAttribute("runCount", runs.size());
        model.addAttribute("latestRuns", runs);
        model.addAttribute("callout", MogMessages.randomCallout());
        model.addAttribute("activePage", "dashboard");
        return "ui/dashboard";
    }

    @GetMapping("/ui/swagger")
    public String swagger(Model model) {
        model.addAttribute("swaggerUrl", properties.getBaseUrl() + "/swagger-ui.html");
        model.addAttribute("activePage", "swagger");
        return "ui/swagger";
    }
}
