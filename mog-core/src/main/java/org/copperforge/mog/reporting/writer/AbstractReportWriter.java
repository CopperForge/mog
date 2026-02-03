package org.copperforge.mog.reporting.writer;

import java.util.HashMap;
import java.util.Map;

import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.definition.Sheet;
import org.copperforge.mog.reporting.element.ElementWriter;
import org.copperforge.mog.reporting.element.ReportElement;

public abstract class AbstractReportWriter implements ReportWriter {

    private String type;

    private final Map<String, ElementWriter<? extends ReportElement>> elementWriters = new HashMap<>();

    public AbstractReportWriter(String type) {
        this.type = type;
    }

    @Override
    public void register() {
        ReportWriterService.instance().register(type, this);
    }

    @Override
    public void build(Report report) throws MogException {
        buildReport(report);
    }

    protected void buildReport(Report report) throws MogException {
        for (Sheet sheet : report.getSheets()) {
            buildSheet(report, sheet);
        }
    }

    protected void buildSheet(Report report, Sheet sheet) throws MogException {
        for (ReportElement element : sheet.getElements()) {
            elementWriters.get(element.getType()).write(report, element);
        }
    }

    public void addElementWriter(String type, ElementWriter<?> writer) {
        if (elementWriters.containsKey(type))
            elementWriters.remove(type);
        elementWriters.put(type, writer);
    }

    public Map<String, ElementWriter<?>> elementWriters() {
        return elementWriters;
    }

}
