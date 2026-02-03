package org.copperforge.mog.reporting.definition;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.xlsx.XLSXReportWriter;
import org.copperforge.mog.runtime.MogCliOptionsView;
import org.copperforge.mog.runtime.MogRuntime;
import org.copperforge.mog.runtime.MogRuntimeContext;
import org.copperforge.mog.var.MogVariableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ReportService {

    private static ReportService _instance = null;
    private final Logger log = LoggerFactory.getLogger(ReportService.class);

    private ReportService() {
        new XLSXReportWriter().register();
    }

    public static final ReportService instance() {
        if (_instance == null)
            _instance = new ReportService();
        return _instance;
    }

    public Report parse(final String filename) throws MogException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            MogVariableService vars = new MogVariableService();
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
            String wd = MogRuntime.current()
                    .flatMap(MogRuntimeContext::cliOptions)
                    .map(MogCliOptionsView::getWorkingDirectory)
                    .orElse(null);
            if (wd != null && !wd.isBlank()) {
                File wdFile = new File(wd, requested);
                if (wdFile.exists()) {
                    try (InputStream in = new FileInputStream(wdFile)) {
                        return objectMapper.readValue(in, Report.class);
                    }
                }
            }

            // 4) Relative to MOG_ETC (if set), then MOG_HOME
            String mogEtc = System.getenv("MOG_ETC");
            if (mogEtc != null && !mogEtc.isBlank()) {
                File etcFile = new File(mogEtc, requested);
                if (etcFile.exists()) {
                    try (InputStream in = new FileInputStream(etcFile)) {
                        return objectMapper.readValue(in, Report.class);
                    }
                }
            }
            String mogHome = System.getenv("MOG_HOME");
            if (mogHome != null && !mogHome.isBlank()) {
                File homeFile = new File(mogHome, requested);
                if (homeFile.exists()) {
                    try (InputStream in = new FileInputStream(homeFile)) {
                        return objectMapper.readValue(in, Report.class);
                    }
                }
            }

            // 5) Search configured report paths
            List<String> paths = MogRuntime.current()
                    .flatMap(MogRuntimeContext::config)
                    .map(cfg -> cfg.getSearchPaths() != null ? cfg.getSearchPaths().getReports() : null)
                    .orElse(Collections.emptyList());
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
