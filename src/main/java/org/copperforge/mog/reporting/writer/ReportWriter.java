package org.copperforge.mog.reporting.writer;

import org.copperforge.mog.reporting.core.ReportException;
import org.copperforge.mog.reporting.definition.Report;

public interface ReportWriter {

    void register();

    void build(Report definition) throws ReportException;

    void save(String filename) throws ReportException;

}
