package org.copperforge.mog.data.catalog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.copperforge.mog.Mog;
import org.copperforge.mog.MogException;
import org.copperforge.mog.MogOptions;
import org.copperforge.mog.data.MogDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads datasources from external catalog files so reports can reference them by name.
 */
public class DataSourcesCatalog {

    private static final Logger log = LoggerFactory.getLogger(DataSourcesCatalog.class);

    private static DataSourcesCatalog INSTANCE;

    private final Map<String, MogDataSource> byName = new HashMap<>();
    private final Map<String, String> sourceByName = new HashMap<>();
    private boolean loaded = false;

    public static synchronized DataSourcesCatalog instance() {
        if (INSTANCE == null) INSTANCE = new DataSourcesCatalog();
        return INSTANCE;
    }

    public MogDataSource resolveByName(String name) {
        try {
            ensureLoaded();
        } catch (Exception e) {
            log.debug("Failed to load datasources catalog", e);
        }
        return byName.get(name);
    }

    private synchronized void ensureLoaded() throws MogException {
        if (loaded) return;
        loaded = true;
        byName.clear();
        sourceByName.clear();

        List<File> candidates = new ArrayList<>();
        FilenameFilter mogFilter = (dir, fname) -> fname.endsWith(".mog");
        MogOptions opts = Mog.mog() != null ? Mog.mog().options() : null;
        String env = null;
        if (opts != null && opts.getEnv() != null && !opts.getEnv().isBlank()) {
            env = opts.getEnv();
        } else {
            String envVar = System.getenv("MOG_ENV");
            if (envVar != null && !envVar.isBlank()) env = envVar;
        }

        // 1) --datasources path, if provided
        if (opts != null && opts.getDatasourcesPath() != null && !opts.getDatasourcesPath().isBlank()) {
            File ds = new File(opts.getDatasourcesPath());
            if (ds.isDirectory()) {
                // generic first
                listSorted(ds, mogFilter, candidates);
                // env-specific overrides
                if (env != null) {
                    File envFile = new File(ds, "datasources." + env + ".mog");
                    if (envFile.isFile()) candidates.add(envFile);
                    File envDir = new File(ds, "datasources/" + env);
                    if (envDir.isDirectory()) listSorted(envDir, mogFilter, candidates);
                }
            } else if (ds.isFile()) {
                candidates.add(ds);
            }
        }

        // 2) ${MOG_ETC}
        String mogEtc = System.getenv("MOG_ETC");
        if (mogEtc != null && !mogEtc.isBlank()) {
            File etc = new File(mogEtc);
            addDefaultLocations(etc, candidates, mogFilter, env);
        }

        // 3) ${MOG_HOME}/etc
        String mogHome = System.getenv("MOG_HOME");
        if (mogHome != null && !mogHome.isBlank()) {
            File etc = new File(mogHome, "etc");
            addDefaultLocations(etc, candidates, mogFilter, env);
        }

        // Load in order; later files override earlier definitions
        for (File f : candidates) {
            loadFile(f);
        }
    }

    private void addDefaultLocations(File etcDir, List<File> out, FilenameFilter mogFilter, String env) {
        // generic first
        File dsFile = new File(etcDir, "datasources.mog");
        if (dsFile.isFile()) out.add(dsFile);
        File dsDir = new File(etcDir, "datasources");
        if (dsDir.isDirectory()) listSorted(dsDir, mogFilter, out);

        // env-specific overrides
        if (env != null) {
            File envFile = new File(etcDir, "datasources." + env + ".mog");
            if (envFile.isFile()) out.add(envFile);
            File envDir = new File(etcDir, "datasources/" + env);
            if (envDir.isDirectory()) listSorted(envDir, mogFilter, out);
        }
    }

    private void listSorted(File dir, FilenameFilter filter, List<File> out) {
        File[] files = dir.listFiles(filter);
        if (files != null) {
            java.util.Arrays.sort(files, java.util.Comparator.comparing(File::getName));
            for (File f : files) out.add(f);
        }
    }

    private void loadFile(File file) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            try (InputStream in = new FileInputStream(file)) {
                DataSourcesFile ds = mapper.readValue(in, DataSourcesFile.class);
                if (ds != null && ds.getDatasources() != null) {
                    ds.getDatasources().forEach(d -> {
                        if (d.getName() != null) {
                            String name = d.getName();
                            if (byName.containsKey(name)) {
                                String prev = sourceByName.get(name);
                                log.warn("Overriding datasource '{}' from {} with {}", name, prev, file.getAbsolutePath());
                            }
                            byName.put(name, d);
                            sourceByName.put(name, file.getAbsolutePath());
                        }
                    });
                }
            }
            log.debug("Loaded datasources from {} (total now: {})", file, byName.size());
        } catch (Exception e) {
            log.warn("Unable to load datasources from {} :: {}", file, e.getMessage());
        }
    }
}
