package org.copperforge.mog.api.storage;

import java.io.IOException;

import org.copperforge.mog.api.config.MogApiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class StorageConfiguration {

    @Bean
    public RunRepository runRepository(MogApiProperties properties, FileSystemStorageLayout layout,
            ObjectMapper objectMapper) throws IOException {
        if (properties.usesJdbcRunMetadataStore()) {
            return new JdbcRunRepository(layout, properties, objectMapper);
        }
        return new FileSystemRunRepository(layout, objectMapper);
    }
}
