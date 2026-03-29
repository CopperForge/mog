package org.copperforge.mog.api.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Repository;

@Repository
public class FileSystemRunRepository implements RunRepository {

    private final FileSystemStorageLayout layout;

    public FileSystemRunRepository(FileSystemStorageLayout layout) {
        this.layout = layout;
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
    public Path metadataFile(String runId) {
        return layout.metadataFile(runId);
    }

    @Override
    public List<String> listRunIds() throws IOException {
        if (!Files.exists(layout.runsDir())) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(layout.runsDir())) {
            return stream
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted(Comparator.reverseOrder())
                    .toList();
        }
    }
}
