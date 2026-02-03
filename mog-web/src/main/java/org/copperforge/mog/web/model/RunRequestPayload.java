package org.copperforge.mog.web.model;

import java.util.Map;

public record RunRequestPayload(
        String reportId,
        String datasourceId,
        Map<String, Object> params,
        Output output) {

    public record Output(String format) {}
}
