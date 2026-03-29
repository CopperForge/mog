package org.copperforge.mog.web.model;

public enum DslResourceType {
    DATASOURCE(
            "datasources",
            "Datasource",
            "datasources",
            "MOG Console · Datasources",
            "Existing datasources",
            "Upload datasource DSL",
            "No datasources yet. Upload one to get started.",
            "Save datasource",
            "Paste datasource JSON here...",
            "Either paste JSON or upload a file below.",
            10),
    REPORT(
            "reports",
            "Report",
            "reports",
            "MOG Console · Reports",
            "Existing reports",
            "Upload report DSL",
            "No reports uploaded yet.",
            "Save report",
            "Paste report JSON here...",
            null,
            12);

    private final String pathSegment;
    private final String singularLabel;
    private final String activePage;
    private final String pageTitle;
    private final String listTitle;
    private final String uploadTitle;
    private final String emptyMessage;
    private final String submitLabel;
    private final String textareaPlaceholder;
    private final String textareaHelp;
    private final int textareaRows;

    DslResourceType(String pathSegment, String singularLabel, String activePage, String pageTitle, String listTitle,
            String uploadTitle, String emptyMessage, String submitLabel, String textareaPlaceholder,
            String textareaHelp, int textareaRows) {
        this.pathSegment = pathSegment;
        this.singularLabel = singularLabel;
        this.activePage = activePage;
        this.pageTitle = pageTitle;
        this.listTitle = listTitle;
        this.uploadTitle = uploadTitle;
        this.emptyMessage = emptyMessage;
        this.submitLabel = submitLabel;
        this.textareaPlaceholder = textareaPlaceholder;
        this.textareaHelp = textareaHelp;
        this.textareaRows = textareaRows;
    }

    public String getPathSegment() {
        return pathSegment;
    }

    public String getSingularLabel() {
        return singularLabel;
    }

    public String getActivePage() {
        return activePage;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public String getListTitle() {
        return listTitle;
    }

    public String getUploadTitle() {
        return uploadTitle;
    }

    public String getEmptyMessage() {
        return emptyMessage;
    }

    public String getSubmitLabel() {
        return submitLabel;
    }

    public String getTextareaPlaceholder() {
        return textareaPlaceholder;
    }

    public String getTextareaHelp() {
        return textareaHelp;
    }

    public int getTextareaRows() {
        return textareaRows;
    }
}
