package org.copperforge.mog.api.storage;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.file.Path;

import org.copperforge.mog.api.config.MogApiProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

class StorageConfigurationTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultsToFileSystemRunRepository() throws Exception {
        MogApiProperties properties = new MogApiProperties();
        properties.setStoreDir(tempDir.resolve("store-file"));
        FileSystemStorageLayout layout = new FileSystemStorageLayout(properties);

        RunRepository repository = new StorageConfiguration()
                .runRepository(properties, layout, new ObjectMapper().findAndRegisterModules());

        assertInstanceOf(FileSystemRunRepository.class, repository);
    }

    @Test
    void selectsJdbcRunRepositoryWhenConfigured() throws Exception {
        MogApiProperties properties = new MogApiProperties();
        properties.setStoreDir(tempDir.resolve("store-jdbc"));
        properties.getRunMetadataStore().setType("jdbc");
        properties.getRunMetadataStore().setJdbcUrl("jdbc:h2:mem:mog-storage-config;DB_CLOSE_DELAY=-1");
        FileSystemStorageLayout layout = new FileSystemStorageLayout(properties);

        RunRepository repository = new StorageConfiguration()
                .runRepository(properties, layout, new ObjectMapper().findAndRegisterModules());

        assertInstanceOf(JdbcRunRepository.class, repository);
    }
}
