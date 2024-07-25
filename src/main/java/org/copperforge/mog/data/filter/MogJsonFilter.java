package org.copperforge.mog.data.filter;

public class MogJsonFilter extends MogDataFilter {

    private String jsonPath;

    public MogJsonFilter(String jsonPath) {
        this.jsonPath = jsonPath;
    }

    public String getJsonPath() {
        return jsonPath;
    }

    public void setJsonPath(String jsonPath) {
        this.jsonPath = jsonPath;
    }

    @Override
    public String toString() {
        return "MogJsonFilter [jsonPath=" + jsonPath + "]";
    }
    
}
