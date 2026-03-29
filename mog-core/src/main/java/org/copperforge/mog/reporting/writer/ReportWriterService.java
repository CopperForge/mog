package org.copperforge.mog.reporting.writer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.copperforge.mog.reporting.xlsx.XLSXReportWriter;

public class ReportWriterService {

    private static ReportWriterService _instance;
    private final Map<String, Supplier<? extends ReportWriter>> builders = new ConcurrentHashMap<>();

    private ReportWriterService() {
        register("xlsx", XLSXReportWriter::new);
    }

    public static final ReportWriterService instance() {
        if (_instance == null)
            _instance = new ReportWriterService();
        return _instance;
    }

    public List<ReportWriter> builders() {
        List<ReportWriter> writers = new ArrayList<>();
        for (Supplier<? extends ReportWriter> factory : builders.values()) {
            writers.add(factory.get());
        }
        return List.copyOf(writers);
    }

    public ReportWriter builder(String type) {
        Supplier<? extends ReportWriter> factory = builders.get(type);
        return factory != null ? factory.get() : null;
    }

    public void register(String type, Supplier<? extends ReportWriter> builder) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        if (builder == null) {
            throw new IllegalArgumentException("builder must not be null");
        }
        builders.put(type, builder);
    }
}
