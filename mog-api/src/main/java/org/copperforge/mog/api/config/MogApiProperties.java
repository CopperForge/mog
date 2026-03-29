package org.copperforge.mog.api.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mog.api")
public class MogApiProperties {

    private Path storeDir;
    private String environment;
    private String mogHome;
    private String mogEtc;
    private RunMetadataStore runMetadataStore = new RunMetadataStore();

    public Path getStoreDir() {
        return storeDir;
    }

    public void setStoreDir(Path storeDir) {
        this.storeDir = storeDir;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getMogHome() {
        return mogHome;
    }

    public void setMogHome(String mogHome) {
        this.mogHome = mogHome;
    }

    public String getMogEtc() {
        return mogEtc;
    }

    public void setMogEtc(String mogEtc) {
        this.mogEtc = mogEtc;
    }

    public RunMetadataStore getRunMetadataStore() {
        return runMetadataStore;
    }

    public void setRunMetadataStore(RunMetadataStore runMetadataStore) {
        this.runMetadataStore = runMetadataStore != null ? runMetadataStore : new RunMetadataStore();
    }

    public Path resolvedStoreDir() {
        if (storeDir != null) {
            return storeDir.toAbsolutePath().normalize();
        }
        String etc = resolvedMogEtc();
        if (etc != null && !etc.isBlank()) {
            return Paths.get(etc, "api-store").toAbsolutePath().normalize();
        }
        return Paths.get("etc", "api-store").toAbsolutePath().normalize();
    }

    public String resolvedEnvironment() {
        if (!isBlank(environment)) {
            return environment;
        }
        String env = System.getenv("MOG_ENV");
        return isBlank(env) ? null : env;
    }

    public String resolvedMogHome() {
        if (!isBlank(mogHome)) {
            return mogHome;
        }
        String env = System.getenv("MOG_HOME");
        return isBlank(env) ? null : env;
    }

    public String resolvedMogEtc() {
        if (!isBlank(mogEtc)) {
            return mogEtc;
        }
        String env = System.getenv("MOG_ETC");
        return isBlank(env) ? null : env;
    }

    public boolean usesJdbcRunMetadataStore() {
        return "jdbc".equalsIgnoreCase(runMetadataStore.getType());
    }

    public String resolvedRunMetadataJdbcUrl() {
        if (!usesJdbcRunMetadataStore()) {
            return null;
        }
        if (!isBlank(runMetadataStore.getJdbcUrl())) {
            return runMetadataStore.getJdbcUrl();
        }
        String defaultPath = resolvedStoreDir().resolve("runs-db").toAbsolutePath().normalize().toString()
                .replace("\\", "/");
        return "jdbc:h2:file:" + defaultPath;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static class RunMetadataStore {
        private String type = "file";
        private String jdbcUrl;
        private String jdbcUser;
        private String jdbcPassword;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getJdbcUser() {
            return jdbcUser;
        }

        public void setJdbcUser(String jdbcUser) {
            this.jdbcUser = jdbcUser;
        }

        public String getJdbcPassword() {
            return jdbcPassword;
        }

        public void setJdbcPassword(String jdbcPassword) {
            this.jdbcPassword = jdbcPassword;
        }
    }
}
