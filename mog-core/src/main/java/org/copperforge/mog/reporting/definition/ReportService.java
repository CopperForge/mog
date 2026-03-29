package org.copperforge.mog.reporting.definition;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.copperforge.mog.MogException;
import org.copperforge.mog.runtime.MogContext;
import org.copperforge.mog.var.MogVariableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ReportService {

    private static ReportService _instance = null;
    private final Logger log = LoggerFactory.getLogger(ReportService.class);

    private ReportService() {
    }

    public static final ReportService instance() {
        if (_instance == null)
            _instance = new ReportService();
        return _instance;
    }

    public Report parse(final String filename) throws MogException {
        return parse(filename, null);
    }

    public Report parse(final String filename, MogContext context) throws MogException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            MogVariableService vars = new MogVariableService(context);
            String requested = vars.envsubst(filename);
            log.info("parsing " + requested);

            // 1) Absolute path
            File file = new File(requested);
            if (file.isAbsolute() && file.exists()) {
                try (InputStream in = new FileInputStream(file)) {
                    return objectMapper.readValue(in, Report.class);
                }
            }

            // 2) As provided (relative to current working dir)
            if (file.exists()) {
                try (InputStream in = new FileInputStream(file)) {
                    return objectMapper.readValue(in, Report.class);
                }
            }

            // 3) Relative to --working-dir, if provided
            String wd = context != null ? context.getWorkingDirectory() : null;
            if (wd != null && !wd.isBlank()) {
                File wdFile = new File(wd, requested);
                if (wdFile.exists()) {
                    try (InputStream in = new FileInputStream(wdFile)) {
                        return objectMapper.readValue(in, Report.class);
                    }
                }
            }

            // 4) Relative to MOG_ETC (if set), then MOG_HOME
            String mogEtc = context != null && context.getMogEtc() != null && !context.getMogEtc().isBlank()
                    ? context.getMogEtc()
                    : System.getenv("MOG_ETC");
            if (mogEtc != null && !mogEtc.isBlank()) {
                File etcFile = new File(mogEtc, requested);
                if (etcFile.exists()) {
                    try (InputStream in = new FileInputStream(etcFile)) {
                        return objectMapper.readValue(in, Report.class);
                    }
                }
            }
            String mogHome = context != null && context.getMogHome() != null && !context.getMogHome().isBlank()
                    ? context.getMogHome()
                    : System.getenv("MOG_HOME");
            if (mogHome != null && !mogHome.isBlank()) {
                File homeFile = new File(mogHome, requested);
                if (homeFile.exists()) {
                    try (InputStream in = new FileInputStream(homeFile)) {
                        return objectMapper.readValue(in, Report.class);
                    }
                }
            }

            // 5) Search configured report paths
            List<String> paths = Collections.emptyList();
            if (context != null && context.getConfig() != null && context.getConfig().getSearchPaths() != null) {
                List<String> configured = context.getConfig().getSearchPaths().getReports();
                if (configured != null) {
                    paths = configured;
                }
            }
            for (String path : paths) {
                String base = vars.envsubst(path);
                File candidate = new File(base, requested);
                if (candidate.exists()) {
                    try (InputStream in = new FileInputStream(candidate)) {
                        return objectMapper.readValue(in, Report.class);
                    }
                }
            }

            throw new MogException("Unable to find report mog for '" + filename + "'");
        } catch (Exception e) {
            throw new MogException(e);
        }
    }

}
