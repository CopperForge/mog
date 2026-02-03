package org.copperforge.mog.api.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

import org.copperforge.mog.api.config.MogApiProperties;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class DslStorageService {

    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Pattern RUN_ID = Pattern.compile("[A-Za-z0-9-]+");

    private final ObjectMapper objectMapper;
    private final Path storeRoot;
    private final Path datasourcesDir;
    private final Path reportsDir;
    private final Path runsDir;

    public DslStorageService(ObjectMapper objectMapper, MogApiProperties properties) throws IOException {
        this.objectMapper = objectMapper;
        this.storeRoot = properties.resolvedStoreDir();
        this.datasourcesDir = storeRoot.resolve("datasources");
        this.reportsDir = storeRoot.resolve("reports");
        this.runsDir = storeRoot.resolve("runs");
        Files.createDirectories(this.datasourcesDir);
        Files.createDirectories(this.reportsDir);
        Files.createDirectories(this.runsDir);
    }

    public SaveResult saveDatasource(JsonNode json) throws IOException {
        return save(json, datasourcesDir);
    }

    public SaveResult saveReport(JsonNode json) throws IOException {
        return save(json, reportsDir);
    }

    public Path requireDatasource(String id) throws IOException {
        return requireDslFile(datasourcesDir, id);
    }

    public Path requireReport(String id) throws IOException {
        return requireDslFile(reportsDir, id);
    }

    public Path ensureRunDirectory(String runId) throws IOException {
        String safe = sanitizeRunId(runId);
        Path dir = runsDir.resolve(safe);
        Files.createDirectories(dir);
        return dir;
    }

    public Path runDirectory(String runId) {
        String safe = sanitizeRunId(runId);
        return runsDir.resolve(safe);
    }

    public Path metadataFile(String runId) {
        return runDirectory(runId).resolve("meta.json");
    }

    public Path getStoreRoot() {
        return storeRoot;
    }

    private SaveResult save(JsonNode json, Path targetDir) throws IOException {
        if (json == null || json.isNull()) {
            throw new IllegalArgumentException("DSL payload must not be empty");
        }
        String id = determineId(json);
        Path file = targetDir.resolve(id + ".json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), json);
        return new SaveResult(id, file);
    }

    private String determineId(JsonNode json) throws IOException {
        JsonNode idNode = json.get("id");
        if (idNode != null && idNode.isTextual()) {
            return sanitizeId(idNode.asText());
        }
        byte[] bytes = objectMapper.writeValueAsBytes(json);
        String generated = UUID.nameUUIDFromBytes(bytes).toString();
        if (json instanceof ObjectNode objectNode) {
            objectNode.put("id", generated);
        }
        return generated;
    }

    private Path requireDslFile(Path baseDir, String id) throws IOException {
        String safeId = sanitizeId(id);
        Path file = baseDir.resolve(safeId + ".json");
        if (!Files.exists(file)) {
            throw new NoSuchFileException("No DSL found for id '" + safeId + "'");
        }
        return file;
    }

    private String sanitizeId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        String candidate = id.trim();
        if (candidate.isEmpty() || !VALID_ID.matcher(candidate).matches()) {
            throw new IllegalArgumentException("Invalid id; use alphanumeric, dash, underscore, or dot characters");
        }
        return candidate;
    }

    private String sanitizeRunId(String runId) {
        if (runId == null) {
            throw new IllegalArgumentException("runId must not be null");
        }
        String candidate = runId.trim();
        if (candidate.isEmpty() || !RUN_ID.matcher(candidate).matches()) {
            throw new IllegalArgumentException("Invalid runId");
        }
        return candidate;
    }

    public record SaveResult(String id, Path file) {}
}
