package org.copperforge.mog.api.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.copperforge.mog.api.run.RunMetadata;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FileSystemRunRepository implements RunRepository {

    private final FileSystemStorageLayout layout;
    private final ObjectMapper objectMapper;

    public FileSystemRunRepository(FileSystemStorageLayout layout, ObjectMapper objectMapper) {
        this.layout = layout;
        this.objectMapper = objectMapper;
    }

    @Override
    public Path createRunDirectory(String runId) throws IOException {
        Path dir = layout.runDirectory(runId);
        Files.createDirectories(dir);
        return dir;
    }

    @Override
    public Path runDirectory(String runId) {
        return layout.runDirectory(runId);
    }

    @Override
    public void saveMetadata(RunMetadata metadata) throws IOException {
        Path runDir = createRunDirectory(metadata.getRunId());
        Path metaFile = runDir.resolve("meta.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(metaFile.toFile(), metadata);
    }

    @Override
    public RunMetadata loadMetadata(String runId) throws IOException {
        Path metaFile = runDirectory(runId).resolve("meta.json");
        if (!Files.exists(metaFile)) {
            throw new NoSuchFileException("Run '" + runId + "' not found");
        }
        return objectMapper.readValue(metaFile.toFile(), RunMetadata.class);
    }

    @Override
    public List<RunMetadata> listMetadata() throws IOException {
        if (!Files.exists(layout.runsDir())) {
            return List.of();
        }
        List<RunMetadata> metadata = new ArrayList<>();
        try (Stream<Path> stream = Files.list(layout.runsDir())) {
            List<String> runIds = stream
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted(Comparator.reverseOrder())
                    .toList();
            for (String runId : runIds) {
                try {
                    metadata.add(loadMetadata(runId));
                } catch (NoSuchFileException ex) {
                    // run directory without metadata; skip
                }
            }
        }
        return metadata;
    }
}
