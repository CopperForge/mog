package org.copperforge.mog.api.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.copperforge.mog.api.config.MogApiProperties;
import org.springframework.stereotype.Component;

@Component
public class FileSystemStorageLayout {

    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Pattern RUN_ID = Pattern.compile("[A-Za-z0-9-]+");

    private final Path storeRoot;
    private final Path datasourcesDir;
    private final Path reportsDir;
    private final Path runsDir;

    public FileSystemStorageLayout(MogApiProperties properties) throws IOException {
        this.storeRoot = properties.resolvedStoreDir();
        this.datasourcesDir = storeRoot.resolve("datasources");
        this.reportsDir = storeRoot.resolve("reports");
        this.runsDir = storeRoot.resolve("runs");
        Files.createDirectories(this.datasourcesDir);
        Files.createDirectories(this.reportsDir);
        Files.createDirectories(this.runsDir);
    }

    public Path getStoreRoot() {
        return storeRoot;
    }

    public Path datasourcesDir() {
        return datasourcesDir;
    }

    public Path reportsDir() {
        return reportsDir;
    }

    public Path runsDir() {
        return runsDir;
    }

    public Path datasourceFile(String id) {
        return datasourcesDir.resolve(sanitizeId(id) + ".json");
    }

    public Path reportFile(String id) {
        return reportsDir.resolve(sanitizeId(id) + ".json");
    }

    public Path runDirectory(String runId) {
        return runsDir.resolve(sanitizeRunId(runId));
    }

    public Path metadataFile(String runId) {
        return runDirectory(runId).resolve("meta.json");
    }

    public String sanitizeId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        String candidate = id.trim();
        if (candidate.isEmpty() || !VALID_ID.matcher(candidate).matches()) {
            throw new IllegalArgumentException("Invalid id; use alphanumeric, dash, underscore, or dot characters");
        }
        return candidate;
    }

    public String sanitizeRunId(String runId) {
        if (runId == null) {
            throw new IllegalArgumentException("runId must not be null");
        }
        String candidate = runId.trim();
        if (candidate.isEmpty() || !RUN_ID.matcher(candidate).matches()) {
            throw new IllegalArgumentException("Invalid runId");
        }
        return candidate;
    }
}
