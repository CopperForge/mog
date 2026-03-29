package org.copperforge.mog.api.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Repository
public class FileSystemDslRepository implements DslRepository {

    private final ObjectMapper objectMapper;
    private final FileSystemStorageLayout layout;

    public FileSystemDslRepository(ObjectMapper objectMapper, FileSystemStorageLayout layout) {
        this.objectMapper = objectMapper;
        this.layout = layout;
    }

    @Override
    public SaveResult saveDatasource(JsonNode json) throws IOException {
        return save(json, layout.datasourcesDir());
    }

    @Override
    public SaveResult saveReport(JsonNode json) throws IOException {
        return save(json, layout.reportsDir());
    }

    @Override
    public Path requireDatasource(String id) throws IOException {
        return requireFile(layout.datasourceFile(id), layout.sanitizeId(id));
    }

    @Override
    public Path requireReport(String id) throws IOException {
        return requireFile(layout.reportFile(id), layout.sanitizeId(id));
    }

    @Override
    public List<String> listDatasourceIds() throws IOException {
        return listIds(layout.datasourcesDir());
    }

    @Override
    public List<String> listReportIds() throws IOException {
        return listIds(layout.reportsDir());
    }

    @Override
    public JsonNode loadDatasource(String id) throws IOException {
        return objectMapper.readTree(requireDatasource(id).toFile());
    }

    @Override
    public JsonNode loadReport(String id) throws IOException {
        return objectMapper.readTree(requireReport(id).toFile());
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
            return layout.sanitizeId(idNode.asText());
        }
        byte[] bytes = objectMapper.writeValueAsBytes(json);
        String generated = UUID.nameUUIDFromBytes(bytes).toString();
        if (json instanceof ObjectNode objectNode) {
            objectNode.put("id", generated);
        }
        return generated;
    }

    private Path requireFile(Path file, String id) throws IOException {
        if (!Files.exists(file)) {
            throw new NoSuchFileException("No DSL found for id '" + id + "'");
        }
        return file;
    }

    private List<String> listIds(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return Collections.emptyList();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json"))
                    .map(path -> {
                        String filename = path.getFileName().toString();
                        return filename.substring(0, filename.length() - ".json".length());
                    })
                    .sorted()
                    .collect(Collectors.toList());
        }
    }
}
