package org.copperforge.mog.reporting;

import org.copperforge.mog.MogOptions;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class ReportOptions extends MogOptions {

    @Option(names = "--report", description = "specify the report config")
    private String report = null;

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    @Override
    public String toString() {
        return "ReportingOptions [report=" + report + ", isHelpRequested()=" + isHelpRequested()
                + ", getConfigFile()=" + getConfigFile() + ", getCommands()=" + getCommands() + "]";
    }

    public static ReportOptions parse(MogOptions options) {
        ReportOptions reportOptions = new ReportOptions();
        new CommandLine(reportOptions).parseArgs(options.rawArgs().toArray(new String[0]));
        return reportOptions;
    }
}

