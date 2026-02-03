package org.copperforge.mog.api;

import org.copperforge.mog.api.config.MogApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MogApiProperties.class)
public class MogApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MogApiApplication.class, args);
    }
}
