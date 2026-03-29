package org.copperforge.mog.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.copperforge.mog.contract.run.RunRequest;
import org.copperforge.mog.contract.run.RunResponse;
import org.copperforge.mog.contract.web.SaveDslResponse;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ContractSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void runRequest_serializesStablePropertyNames() throws Exception {
        RunRequest request = new RunRequest("report-a", "ds-a", Map.of("customer", "acme"),
                new RunRequest.RunOutput("XLSX"));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(request));

        assertEquals("report-a", json.path("reportId").asText());
        assertEquals("ds-a", json.path("datasourceId").asText());
        assertEquals("acme", json.path("params").path("customer").asText());
        assertEquals("XLSX", json.path("output").path("format").asText());
    }

    @Test
    void responses_serializeStablePropertyNames() throws Exception {
        JsonNode runResponse = objectMapper.readTree(objectMapper.writeValueAsBytes(new RunResponse("run-1", "STARTED")));
        JsonNode saveResponse = objectMapper.readTree(objectMapper.writeValueAsBytes(new SaveDslResponse("report-a", true)));

        assertEquals("run-1", runResponse.path("runId").asText());
        assertEquals("STARTED", runResponse.path("status").asText());
        assertEquals("report-a", saveResponse.path("id").asText());
        assertEquals(true, saveResponse.path("saved").asBoolean());
    }
}
