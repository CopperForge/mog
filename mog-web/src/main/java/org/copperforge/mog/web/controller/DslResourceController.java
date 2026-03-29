package org.copperforge.mog.web.controller;

import org.copperforge.mog.contract.web.SaveDslResponse;
import org.copperforge.mog.web.model.DslResourceType;
import org.copperforge.mog.web.model.DslUploadForm;
import org.copperforge.mog.web.service.DslResourcePageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DslResourceController {

    private final DslResourcePageService pageService;

    public DslResourceController(DslResourcePageService pageService) {
        this.pageService = pageService;
    }

    @ModelAttribute("form")
    public DslUploadForm form() {
        return new DslUploadForm();
    }

    @GetMapping("/ui/datasources")
    public String datasources(Model model) {
        return show(DslResourceType.DATASOURCE, model);
    }

    @PostMapping("/ui/datasources")
    public String uploadDatasource(@ModelAttribute("form") DslUploadForm form, RedirectAttributes redirectAttributes) {
        return upload(DslResourceType.DATASOURCE, form, redirectAttributes);
    }

    @GetMapping("/ui/datasources/{id}")
    public String viewDatasource(@PathVariable String id, Model model) throws Exception {
        return view(DslResourceType.DATASOURCE, id, model);
    }

    @GetMapping("/ui/reports")
    public String reports(Model model) {
        return show(DslResourceType.REPORT, model);
    }

    @PostMapping("/ui/reports")
    public String uploadReport(@ModelAttribute("form") DslUploadForm form, RedirectAttributes redirectAttributes) {
        return upload(DslResourceType.REPORT, form, redirectAttributes);
    }

    @GetMapping("/ui/reports/{id}")
    public String viewReport(@PathVariable String id, Model model) throws Exception {
        return view(DslResourceType.REPORT, id, model);
    }

    private String show(DslResourceType type, Model model) {
        populate(type, model);
        return "ui/dsl-resource";
    }

    private String view(DslResourceType type, String id, Model model) throws Exception {
        populate(type, model);
        model.addAttribute("selectedJson", pageService.loadPrettyJson(type, id));
        model.addAttribute("selectedId", id);
        return "ui/dsl-resource";
    }

    private String upload(DslResourceType type, DslUploadForm form, RedirectAttributes redirectAttributes) {
        SaveDslResponse response = pageService.save(type, form);
        redirectAttributes.addFlashAttribute("successMessage", type.getSingularLabel() + " " + response.id() + " saved");
        redirectAttributes.addAttribute("id", response.id());
        return "redirect:/ui/" + type.getPathSegment() + "/{id}";
    }

    private void populate(DslResourceType type, Model model) {
        model.addAttribute("resourceType", type);
        model.addAttribute("resourceIds", pageService.listIds(type));
        model.addAttribute("resourceBasePath", "/ui/" + type.getPathSegment());
        model.addAttribute("activePage", type.getActivePage());
    }
}
