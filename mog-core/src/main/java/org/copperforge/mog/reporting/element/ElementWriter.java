package org.copperforge.mog.reporting.element;

import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.definition.Report;

public interface ElementWriter<T extends ReportElement> {

    void write(Report report, ReportElement element) throws MogException;

}
