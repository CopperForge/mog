package org.copperforge.mog.reporting.datasource.dotout;

import java.util.List;

import org.copperforge.mog.reporting.core.Reportable;
import org.copperforge.mog.reporting.datasource.ReportDataSource;

public class ReportDotOutDataSource extends ReportDataSource {

    private String outFile;

    private String inputFile;

    public String getOutFile() {
        return outFile;
    }

    public void setOutFile(String outFile) {
        this.outFile = outFile;
    }

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    @Override
    public String toString() {
        return "ReportDotOutDataSource [outFile=" + outFile + ", inputFile=" + inputFile + "]";
    }

    @Override
    public List<Reportable> data() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'data'");
    }

    
}
