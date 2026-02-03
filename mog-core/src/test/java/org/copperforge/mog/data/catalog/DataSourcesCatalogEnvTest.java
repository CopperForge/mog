package org.copperforge.mog.data.catalog;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.copperforge.mog.data.MogDataSource;
import org.copperforge.mog.runtime.MogContext;
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
        MogContext context = MogContext.builder()
                .datasourcesPath(tmp.toString())
                .environment("dev")
                .build();
        MogDataSource ds = DataSourcesCatalog.instance().resolveByName("svc", context);
        assertNotNull(ds);
        // Ensure the file path ends with b.json (env-specific)
        String s = ds.toString();
        assertTrue(s.contains("b.json"), () -> "Expected env-specific override to b.json, got: " + s);
    }

    private static void resetCatalog() throws Exception {
        Field f = DataSourcesCatalog.class.getDeclaredField("INSTANCE");
        f.setAccessible(true);
        f.set(null, null);
    }

}
