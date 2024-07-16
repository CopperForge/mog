package org.copperforge.mog.reporting.definition;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.copperforge.mog.Mog;
import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.xlsx.XLSXReportWriter;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ReportService {

    private static ReportService _instance = null;

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
            File file = new File(filename);
            if (file.isAbsolute()) {
                InputStream in = new FileInputStream(file);
                return objectMapper.readValue(in, Report.class);
            } else {
                // search report paths
                List<String> paths = Mog.mog().config().getSearchPaths().getReports();
                for (String path : paths) {
                    String fullpath = path;
                    if (path.endsWith("/") || path.endsWith("\\") || filename.startsWith("/") || filename.startsWith("\\")) {
                        fullpath += filename;
                    } else {
                        fullpath += "/" + filename;
                    }
                    file = new File(fullpath);
                    if (file.exists()) {
                        InputStream in = new FileInputStream(file);
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
