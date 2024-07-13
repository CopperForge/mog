package org.copperforge.mog.reporting.definition;

import java.io.InputStream;

import org.copperforge.mog.MogException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ReportService {

    private static ReportService _instance = null;

    private ReportService() {

    }

    public static final ReportService instance() {
        if (_instance == null)
            _instance = new ReportService();
        return _instance;
    }

    public Report parse(final String filename) throws MogException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            InputStream in = this.getClass().getResourceAsStream(filename);
            return objectMapper.readValue(in, Report.class);
        } catch (Exception e) {
            throw new MogException(e);
        }
    }

}
