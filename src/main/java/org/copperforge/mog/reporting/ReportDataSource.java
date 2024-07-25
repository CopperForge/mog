package org.copperforge.mog.reporting;

import org.copperforge.mog.data.filter.MogDataFilter;

public class ReportDataSource {

    private String name;

    private MogDataFilter filter = null;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MogDataFilter getFilter() {
        return filter;
    }

    public void setFilter(MogDataFilter filter) {
        this.filter = filter;
    }

    @Override
    public String toString() {
        return "ReportDataSource [name=" + name + ", filter=" + filter + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((filter == null) ? 0 : filter.hashCode());
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
        ReportDataSource other = (ReportDataSource) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (filter == null) {
            if (other.filter != null)
                return false;
        } else if (!filter.equals(other.filter))
            return false;
        return true;
    }

}
