package org.copperforge.mog.web.service;

import java.util.List;

import org.copperforge.mog.contract.web.SaveDslResponse;
import org.copperforge.mog.web.client.MogApiClient;
import org.copperforge.mog.web.model.DslResourceType;
import org.copperforge.mog.web.model.DslUploadForm;
import org.copperforge.mog.web.support.JsonHelper;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DslResourcePageService {

    private final MogApiClient client;
    private final JsonHelper jsonHelper;
    private final ObjectMapper objectMapper;

    public DslResourcePageService(MogApiClient client, JsonHelper jsonHelper, ObjectMapper objectMapper) {
        this.client = client;
        this.jsonHelper = jsonHelper;
        this.objectMapper = objectMapper;
    }

    public List<String> listIds(DslResourceType type) {
        return client.listDslResources(type);
    }

    public SaveDslResponse save(DslResourceType type, DslUploadForm form) {
        return client.saveDslResource(type, jsonHelper.parseBody(form.getJsonText(), form.getFile()));
    }

    public String loadPrettyJson(DslResourceType type, String id) throws Exception {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(client.getDslResource(type, id));
    }
}
