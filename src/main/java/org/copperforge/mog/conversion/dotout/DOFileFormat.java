package org.copperforge.mog.conversion.dotout;

import java.util.ArrayList;
import java.util.List;

public class DOFileFormat {

    private String outFile;

    private final List<DOField> fields = new ArrayList<>();

    public String getOutFile() {
        return outFile;
    }

    public void setOutFile(String outFile) {
        this.outFile = outFile;
    }

    public List<DOField> getFields() {
        return fields;
    }

    public void setFields(List<DOField> fields) {
        this.fields.clear();
        this.fields.addAll(fields);
    }

    public int length() { 
        return this.fields.stream().mapToInt(f -> f.getLength()).sum();
    }

    @Override
    public String toString() {
        return "DOFileFormat [outFile=" + outFile + ", size=" + length() + ", fields=" + fields + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((outFile == null) ? 0 : outFile.hashCode());
        result = prime * result + ((fields == null) ? 0 : fields.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DOFileFormat other = (DOFileFormat) obj;
        if (outFile == null) {
            if (other.outFile != null)
                return false;
        } else if (!outFile.equals(other.outFile))
            return false;
        if (fields == null) {
            if (other.fields != null)
                return false;
        } else if (!fields.equals(other.fields))
            return false;
        return true;
    }

    
}
