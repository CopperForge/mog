package org.copperforge.mog.web.model;

import jakarta.validation.constraints.NotBlank;

public class RunForm {

    @NotBlank(message = "Select a report")
    private String reportId;

    @NotBlank(message = "Select a datasource")
    private String datasourceId;

    private String paramsJson;

    private String outputFormat = "XLSX";

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getDatasourceId() {
        return datasourceId;
    }

    public void setDatasourceId(String datasourceId) {
        this.datasourceId = datasourceId;
    }

    public String getParamsJson() {
        return paramsJson;
    }

    public void setParamsJson(String paramsJson) {
        this.paramsJson = paramsJson;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }
}
