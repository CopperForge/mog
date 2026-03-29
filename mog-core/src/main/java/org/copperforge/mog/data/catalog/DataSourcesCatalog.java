package org.copperforge.mog.data.catalog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.copperforge.mog.MogException;
import org.copperforge.mog.data.MogDataSource;
import org.copperforge.mog.runtime.MogContext;
import org.copperforge.mog.runtime.MogRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads datasources from external catalog files so reports can reference them by name.
 */
public class DataSourcesCatalog {

    private static final Logger log = LoggerFactory.getLogger(DataSourcesCatalog.class);

    private static final FilenameFilter DSL_FILTER = (dir, fname) -> {
        String lower = fname.toLowerCase(java.util.Locale.ROOT);
        return lower.endsWith(".mog") || lower.endsWith(".json");
    };

    private static DataSourcesCatalog INSTANCE;
    private final Map<CatalogKey, CatalogState> caches = new ConcurrentHashMap<>();

    public static synchronized DataSourcesCatalog instance() {
        if (INSTANCE == null) INSTANCE = new DataSourcesCatalog();
        return INSTANCE;
    }

    public MogDataSource resolveByName(String name) {
        return resolveByName(name, null);
    }

    public MogDataSource resolveByName(String name, MogContext context) {
        if (name == null) return null;
        CatalogKey key = CatalogKey.from(context);
        CatalogState state = caches.computeIfAbsent(key, k -> new CatalogState());
        try {
            state.ensureLoaded(context);
        } catch (Exception e) {
            log.debug("Failed to load datasources catalog for {}", key, e);
        }
        return state.byName.get(name);
    }

    private static final class CatalogState {
        private final Map<String, MogDataSource> byName = new HashMap<>();
        private final Map<String, String> sourceByName = new HashMap<>();
        private boolean loaded = false;

        synchronized void ensureLoaded(MogContext context) throws MogException {
            if (loaded) return;
            loaded = true;
            List<File> candidates = buildCandidates(context);
            for (File f : candidates) {
                loadFile(f, byName, sourceByName);
            }
        }
    }

    private static List<File> buildCandidates(MogContext context) {
        List<File> candidates = new ArrayList<>();
        String env = resolveEnv(context);

        String datasourcesPath = context != null ? context.getDatasourcesPath() : null;
        if (datasourcesPath != null && !datasourcesPath.isBlank()) {
            File ds = new File(datasourcesPath);
            if (ds.isDirectory()) {
                listSorted(ds, candidates);
                if (env != null && !env.isBlank()) {
                    File envFile = new File(ds, "datasources." + env + ".mog");
                    if (envFile.isFile()) candidates.add(envFile);
                    File envJson = new File(ds, "datasources." + env + ".json");
                    if (envJson.isFile()) candidates.add(envJson);
                    File envDir = new File(ds, "datasources/" + env);
                    if (envDir.isDirectory()) listSorted(envDir, candidates);
                }
            } else if (ds.isFile()) {
                candidates.add(ds);
            }
        }

        String mogEtc = context != null && context.getMogEtc() != null && !context.getMogEtc().isBlank()
                ? context.getMogEtc()
                : System.getenv("MOG_ETC");
        if (mogEtc != null && !mogEtc.isBlank()) {
            File etc = new File(mogEtc);
            addDefaultLocations(etc, candidates, env);
        }

        String mogHome = context != null && context.getMogHome() != null && !context.getMogHome().isBlank()
                ? context.getMogHome()
                : System.getenv("MOG_HOME");
        if (mogHome != null && !mogHome.isBlank()) {
            File etc = new File(mogHome, "etc");
            addDefaultLocations(etc, candidates, env);
        }

        return candidates;
    }

    private static void addDefaultLocations(File etcDir, List<File> out, String env) {
        if (etcDir == null || !etcDir.exists()) return;
        // generic first
        File dsFile = new File(etcDir, "datasources.mog");
        if (dsFile.isFile()) out.add(dsFile);
        File dsJson = new File(etcDir, "datasources.json");
        if (dsJson.isFile()) out.add(dsJson);
        File dsDir = new File(etcDir, "datasources");
        if (dsDir.isDirectory()) listSorted(dsDir, out);

        // env-specific overrides
        if (env != null && !env.isBlank()) {
            File envFile = new File(etcDir, "datasources." + env + ".mog");
            if (envFile.isFile()) out.add(envFile);
            File envJson = new File(etcDir, "datasources." + env + ".json");
            if (envJson.isFile()) out.add(envJson);
            File envDir = new File(etcDir, "datasources/" + env);
            if (envDir.isDirectory()) listSorted(envDir, out);
        }
    }

    private static void listSorted(File dir, List<File> out) {
        File[] files = dir.listFiles(DSL_FILTER);
        if (files != null) {
            java.util.Arrays.sort(files, java.util.Comparator.comparing(File::getName));
            for (File f : files) out.add(f);
        }
    }

    private static void loadFile(File file, Map<String, MogDataSource> byName, Map<String, String> sourceByName) {
        if (file == null || !file.isFile()) return;
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

    private static String resolveEnv(MogContext context) {
        if (context != null && context.getEnvironment() != null && !context.getEnvironment().isBlank()) {
            return context.getEnvironment();
        }
        String envVar = System.getenv("MOG_ENV");
        if (envVar != null && !envVar.isBlank()) return envVar;
        return null;
    }

    private record CatalogKey(String datasourcesPath, String environment, String mogEtc, String mogHome) {
        static CatalogKey from(MogContext context) {
            return new CatalogKey(
                    normalizePath(context != null ? context.getDatasourcesPath() : null),
                    resolveEnv(context),
                    normalizePath(context != null && context.getMogEtc() != null && !context.getMogEtc().isBlank()
                            ? context.getMogEtc()
                            : System.getenv("MOG_ETC")),
                    normalizePath(context != null && context.getMogHome() != null && !context.getMogHome().isBlank()
                            ? context.getMogHome()
                            : System.getenv("MOG_HOME"))
            );
        }

        private static String normalizePath(String path) {
            if (path == null || path.isBlank()) return null;
            return new File(path).getAbsolutePath();
        }
    }
}
