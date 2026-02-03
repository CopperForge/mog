package org.copperforge.mog.web;

import org.copperforge.mog.web.config.MogApiClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MogApiClientProperties.class)
public class MogWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(MogWebApplication.class, args);
    }
}
