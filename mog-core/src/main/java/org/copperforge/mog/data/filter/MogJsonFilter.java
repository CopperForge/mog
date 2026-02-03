package org.copperforge.mog.data.filter;

public class MogJsonFilter extends MogDataFilter {

    private String jsonPath;

    private String suburl;

    private String method;

    public MogJsonFilter() {
    }

    public String getJsonPath() {
        return jsonPath;
    }

    public void setJsonPath(String jsonPath) {
        this.jsonPath = jsonPath;
    }

    public String getSuburl() {
        return suburl;
    }

    public void setSuburl(String suburl) {
        this.suburl = suburl;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((jsonPath == null) ? 0 : jsonPath.hashCode());
        result = prime * result + ((suburl == null) ? 0 : suburl.hashCode());
        result = prime * result + ((method == null) ? 0 : method.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        MogJsonFilter other = (MogJsonFilter) obj;
        if (jsonPath == null) {
            if (other.jsonPath != null)
                return false;
        } else if (!jsonPath.equals(other.jsonPath))
            return false;
        if (suburl == null) {
            if (other.suburl != null)
                return false;
        } else if (!suburl.equals(other.suburl))
            return false;
        if (method == null) {
            if (other.method != null)
                return false;
        } else if (!method.equals(other.method))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "MogJsonFilter [jsonPath=" + jsonPath + ", suburl=" + suburl + ", method=" + method + "]";
    }

    
}
