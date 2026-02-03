package org.copperforge.mog.runtime;

/**
 * Lightweight view of CLI options that core code depends on without pulling in Picocli.
 */
public interface MogCliOptionsView {
    String getDatasourcesPath();
    String getEnv();
    String getWorkingDirectory();
}
