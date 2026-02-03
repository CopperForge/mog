package org.copperforge.mog.reporting.writer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportWriterService {

    private static ReportWriterService _instance;
    private final Map<String, ReportWriter> builders = new HashMap<>();

    private ReportWriterService() {

    }

    public static final ReportWriterService instance() {
        if (_instance == null)
            _instance = new ReportWriterService();
        return _instance;
    }

    public List<ReportWriter> builders() {
        return builders.values().stream().toList();
    }

    public ReportWriter builder(String type) {
        return builders.get(type);
    }

    public void register(String type, ReportWriter builder) {
        if (builders.containsKey(type)) builders.remove(type);
        builders.put(type, builder);
    }

    
}
