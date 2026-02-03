package org.copperforge.mog.web.client;

import java.util.List;
import java.util.Map;

import org.copperforge.mog.web.config.MogApiClientProperties;
import org.copperforge.mog.web.model.ArtifactDownload;
import org.copperforge.mog.web.model.RunMetadata;
import org.copperforge.mog.web.model.RunRequestPayload;
import org.copperforge.mog.web.model.RunResponse;
import org.copperforge.mog.web.model.RunSummary;
import org.copperforge.mog.web.model.SaveDslResponse;
import org.copperforge.mog.web.support.MogApiClientException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class MogApiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public MogApiClient(RestClient.Builder builder, MogApiClientProperties properties, ObjectMapper objectMapper) {
        this.restClient = builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = objectMapper;
    }

    public List<String> listDatasources() {
        return getList("/api/datasources");
    }

    public List<String> listReports() {
        return getList("/api/reports");
    }

    public JsonNode getDatasource(String id) {
        return getJson("/api/datasources/{id}", Map.of("id", id));
    }

    public JsonNode getReport(String id) {
        return getJson("/api/reports/{id}", Map.of("id", id));
    }

    public SaveDslResponse saveDatasource(JsonNode json) {
        return post("/api/datasources", json, SaveDslResponse.class);
    }

    public SaveDslResponse saveReport(JsonNode json) {
        return post("/api/reports", json, SaveDslResponse.class);
    }

    public RunResponse runReport(RunRequestPayload payload) {
        return post("/api/runs", payload, RunResponse.class);
    }

    public List<RunSummary> listRuns(int limit) {
        return getList("/api/runs?limit=" + limit, new ParameterizedTypeReference<List<RunSummary>>() {});
    }

    public RunMetadata getRun(String id) {
        return get("/api/runs/{id}", Map.of("id", id), RunMetadata.class);
    }

    public ArtifactDownload downloadArtifact(String runId) {
        try {
            ResponseEntity<byte[]> entity = restClient.get()
                    .uri("/api/runs/{id}/artifact", runId)
                    .retrieve()
                    .toEntity(byte[].class);
            HttpHeaders headers = entity.getHeaders();
            ContentDisposition cd = headers.getContentDisposition();
            String filename = cd != null && cd.getFilename() != null ? cd.getFilename() : "artifact.bin";
            String contentType = headers.getContentType() != null ? headers.getContentType().toString()
                    : MediaType.APPLICATION_OCTET_STREAM_VALUE;
            return new ArtifactDownload(entity.getBody(), filename, contentType);
        } catch (RestClientException ex) {
            throw translate("download artifact", ex);
        }
    }

    private List<String> getList(String uri) {
        return getList(uri, new ParameterizedTypeReference<List<String>>() {});
    }

    private <T> List<T> getList(String uri, ParameterizedTypeReference<List<T>> type) {
        try {
            return restClient.get().uri(uri).retrieve().body(type);
        } catch (RestClientException ex) {
            throw translate("fetch list from " + uri, ex);
        }
    }

    private JsonNode getJson(String uri, Map<String, ?> uriVariables) {
        try {
            String raw = restClient.get().uri(uri, uriVariables).retrieve().body(String.class);
            return objectMapper.readTree(raw);
        } catch (RestClientException ex) {
            throw translate("fetch JSON from " + uri, ex);
        } catch (Exception ex) {
            throw new MogApiClientException("Unable to parse JSON response", ex, HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    private <T> T get(String uri, Map<String, ?> uriVariables, Class<T> type) {
        try {
            return restClient.get().uri(uri, uriVariables).retrieve().body(type);
        } catch (RestClientException ex) {
            throw translate("fetch resource from " + uri, ex);
        }
    }

    private <T> T post(String uri, Object body, Class<T> type) {
        try {
            return restClient.post().uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve().body(type);
        } catch (RestClientException ex) {
            throw translate("post to " + uri, ex);
        }
    }

    private MogApiClientException translate(String action, RestClientException ex) {
        if (ex instanceof RestClientResponseException response) {
            return new MogApiClientException("Failed to " + action, response, response.getStatusCode(),
                    response.getResponseBodyAsString());
        }
        return new MogApiClientException("Failed to " + action, ex, HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }
}
