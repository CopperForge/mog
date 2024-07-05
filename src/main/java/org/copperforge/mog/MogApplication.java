package org.copperforge.mog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;

@SpringBootApplication
public class MogApplication implements CommandLineRunner {

    private static Logger _log = LoggerFactory.getLogger(MogApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(MogApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        PropertySource<?> ps = new SimpleCommandLinePropertySource(args);
        System.out.println(ps.containsProperty("bob"));
        for (int i = 0; i < args.length; ++i) {
            _log.info("args[{}]: {}", i, args[i]);
        }
    }

}
