package org.copperforge.mog.api.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import org.copperforge.mog.api.config.MogApiProperties;
import org.copperforge.mog.api.run.RunMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

class FileSystemRunRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void savesLoadsAndListsMetadata() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        MogApiProperties properties = new MogApiProperties();
        properties.setStoreDir(tempDir.resolve("store"));

        FileSystemStorageLayout layout = new FileSystemStorageLayout(properties);
        FileSystemRunRepository repository = new FileSystemRunRepository(layout, objectMapper);

        RunMetadata metadata = RunMetadata.starting("run-1", "sales", "shared", Map.of("year", 2025), "XLSX");
        metadata.setStartedAt(Instant.parse("2026-03-28T12:00:00Z"));
        metadata.markCompleted(new RunMetadata.ArtifactMetadata("artifact.xlsx", "application/test", 42L));

        repository.createRunDirectory("run-1");
        repository.saveMetadata(metadata);

        RunMetadata loaded = repository.loadMetadata("run-1");
        assertEquals("sales", loaded.getReportId());
        assertEquals("shared", loaded.getDatasourceId());
        assertEquals(2025, loaded.getParams().get("year"));
        assertEquals("artifact.xlsx", loaded.getArtifact().getFileName());

        assertEquals(1, repository.listMetadata().size());
        assertNotNull(repository.runDirectory("run-1"));
    }
}
