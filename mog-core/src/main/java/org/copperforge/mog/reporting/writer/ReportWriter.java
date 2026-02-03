package org.copperforge.mog.reporting.writer;

import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.definition.Report;

public interface ReportWriter {

    void register();

    void build(Report definition) throws MogException;

    void save(String filename) throws MogException;

}
