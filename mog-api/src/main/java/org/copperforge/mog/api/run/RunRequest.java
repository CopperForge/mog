package org.copperforge.mog.api.run;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RunRequest(
        @NotBlank String reportId,
        @NotBlank String datasourceId,
        Map<String, Object> params,
        RunOutput output) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record RunOutput(String format) { }
}
