package org.copperforge.mog.data.catalog;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.copperforge.mog.config.MogConfig;
import org.copperforge.mog.config.Mogf;
import org.copperforge.mog.data.MogDataSource;
import org.copperforge.mog.runtime.MogCliOptionsView;
import org.copperforge.mog.runtime.MogEncryptionOptionsView;
import org.copperforge.mog.runtime.MogRuntime;
import org.copperforge.mog.runtime.MogRuntimeContext;
import org.junit.jupiter.api.Test;

public class DataSourcesCatalogEnvTest {

    @Test
    void envSpecific_overrides_generic() throws Exception {
        resetCatalog();
        Path tmp = Files.createTempDirectory("mog-ds-env");
        // generic defines ds 'svc' type json with file A
        Path a = tmp.resolve("a.json");
        Files.writeString(a, "{\"data\":[1]}", StandardCharsets.UTF_8);
        Files.writeString(tmp.resolve("datasources.mog"), "{\n \"datasources\": [ { \"name\": \"svc\", \"type\": \"json\", \"file\": \"" + a.toString().replace("\\", "\\\\") + "\" } ]\n}", StandardCharsets.UTF_8);
        // env-specific dev overrides 'svc' to point at B
        Path b = tmp.resolve("b.json");
        Files.writeString(b, "{\"data\":[1,2,3]}", StandardCharsets.UTF_8);
        Files.writeString(tmp.resolve("datasources.dev.mog"), "{\n \"datasources\": [ { \"name\": \"svc\", \"type\": \"json\", \"file\": \"" + b.toString().replace("\\", "\\\\") + "\" } ]\n}", StandardCharsets.UTF_8);

        // Point catalog at dir and set env=dev
        MogRuntime.register(new TestRuntimeContext(new TestOptions(tmp.toString(), "dev", null)));

        MogDataSource ds = DataSourcesCatalog.instance().resolveByName("svc");
        assertNotNull(ds);
        // Ensure the file path ends with b.json (env-specific)
        String s = ds.toString();
        assertTrue(s.contains("b.json"), () -> "Expected env-specific override to b.json, got: " + s);
        MogRuntime.clear();
    }

    private static void resetCatalog() throws Exception {
        Field f = DataSourcesCatalog.class.getDeclaredField("INSTANCE");
        f.setAccessible(true);
        f.set(null, null);
    }

    private record TestOptions(String datasourcesPath, String env, String workingDir) implements MogCliOptionsView {
        @Override
        public String getDatasourcesPath() {
            return datasourcesPath;
        }

        @Override
        public String getEnv() {
            return env;
        }

        @Override
        public String getWorkingDirectory() {
            return workingDir;
        }
    }

    private record TestRuntimeContext(MogCliOptionsView options) implements MogRuntimeContext {
        @Override
        public Optional<MogConfig> config() {
            return Optional.empty();
        }

        @Override
        public Optional<Mogf> mogf() {
            return Optional.empty();
        }

        @Override
        public Optional<? extends MogCliOptionsView> cliOptions() {
            return Optional.ofNullable(options);
        }

        @Override
        public Optional<? extends MogEncryptionOptionsView> encryptionOptions() {
            return Optional.empty();
        }
    }
}
