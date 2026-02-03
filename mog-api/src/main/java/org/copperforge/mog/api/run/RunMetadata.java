package org.copperforge.mog.api.run;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class RunMetadata {

    private String runId;
    private String reportId;
    private String datasourceId;
    private Map<String, Object> params = new LinkedHashMap<>();
    private String format;
    private RunStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private String message;
    private ArtifactMetadata artifact;

    public static RunMetadata starting(String runId, String reportId, String datasourceId,
            Map<String, Object> params, String format) {
        RunMetadata metadata = new RunMetadata();
        metadata.setRunId(runId);
        metadata.setReportId(reportId);
        metadata.setDatasourceId(datasourceId);
        if (params != null && !params.isEmpty()) {
            metadata.setParams(new LinkedHashMap<>(params));
        }
        metadata.setFormat(format);
        metadata.setStatus(RunStatus.STARTED);
        metadata.setStartedAt(Instant.now());
        return metadata;
    }

    public void markCompleted(ArtifactMetadata artifactMetadata) {
        this.status = RunStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.artifact = artifactMetadata;
    }

    public void markFailed(String failureMessage) {
        this.status = RunStatus.FAILED;
        this.completedAt = Instant.now();
        this.message = failureMessage;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

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

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params != null ? params : new LinkedHashMap<>();
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ArtifactMetadata getArtifact() {
        return artifact;
    }

    public void setArtifact(ArtifactMetadata artifact) {
        this.artifact = artifact;
    }

    public static class ArtifactMetadata {
        private String fileName;
        private String contentType;
        private long size;

        public ArtifactMetadata() {
        }

        public ArtifactMetadata(String fileName, String contentType, long size) {
            this.fileName = fileName;
            this.contentType = contentType;
            this.size = size;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }
}
