package org.copperforge.mog.web.support;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JsonHelper {

    private final ObjectMapper objectMapper;

    public JsonHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode parseBody(String text, MultipartFile file) {
        try {
            if (file != null && !file.isEmpty()) {
                return objectMapper.readTree(file.getInputStream());
            }
            if (text != null && !text.isBlank()) {
                return objectMapper.readTree(text);
            }
            throw new IllegalArgumentException("Provide JSON via upload or textarea");
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JSON payload: " + ex.getMessage(), ex);
        }
    }
}
