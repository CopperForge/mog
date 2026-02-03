package org.copperforge.mog.runtime;

import org.copperforge.mog.config.MogConfig;
import org.copperforge.mog.config.Mogf;

/**
 * Immutable snapshot of runtime settings supplied by the caller.
 */
public final class MogContext {

    private final MogConfig config;
    private final Mogf mogf;
    private final String datasourcesPath;
    private final String environment;
    private final String workingDirectory;
    private final String mogHome;
    private final String mogEtc;

    private MogContext(Builder builder) {
        this.config = builder.config;
        this.mogf = builder.mogf;
        this.datasourcesPath = builder.datasourcesPath;
        this.environment = builder.environment;
        this.workingDirectory = builder.workingDirectory;
        this.mogHome = builder.mogHome;
        this.mogEtc = builder.mogEtc;
    }

    public MogConfig getConfig() {
        return config;
    }

    public Mogf getMogf() {
        return mogf;
    }

    public String getDatasourcesPath() {
        return datasourcesPath;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public String getMogHome() {
        return mogHome;
    }

    public String getMogEtc() {
        return mogEtc;
    }

    public Builder toBuilder() {
        return new Builder()
                .config(this.config)
                .mogf(this.mogf)
                .datasourcesPath(this.datasourcesPath)
                .environment(this.environment)
                .workingDirectory(this.workingDirectory)
                .mogHome(this.mogHome)
                .mogEtc(this.mogEtc);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private MogConfig config;
        private Mogf mogf;
        private String datasourcesPath;
        private String environment;
        private String workingDirectory;
        private String mogHome;
        private String mogEtc;

        public Builder config(MogConfig config) {
            this.config = config;
            return this;
        }

        public Builder mogf(Mogf mogf) {
            this.mogf = mogf;
            return this;
        }

        public Builder datasourcesPath(String datasourcesPath) {
            this.datasourcesPath = datasourcesPath;
            return this;
        }

        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public Builder workingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        public Builder mogHome(String mogHome) {
            this.mogHome = mogHome;
            return this;
        }

        public Builder mogEtc(String mogEtc) {
            this.mogEtc = mogEtc;
            return this;
        }

        public MogContext build() {
            return new MogContext(this);
        }
    }
}
