package org.copperforge.mog.reporting.definition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.copperforge.mog.data.MogDataSource;
import org.copperforge.mog.reporting.xlsx.XLSXReport;
import org.copperforge.mog.runtime.MogContext;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = XLSXReport.class, name = "xlsx"),
})
public class Report implements Serializable {

    private String name;

    private String type;

    private final List<Sheet> sheets = new ArrayList<>();

    private final List<MogDataSource> dataSources = new ArrayList<>();

    private String filename;

    @JsonIgnore
    private transient MogContext context;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Sheet> getSheets() {
        return sheets;
    }

    public void setSheets(List<Sheet> sheets) {
        this.sheets.clear();

        if (sheets != null) this.sheets.addAll(sheets);
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public List<MogDataSource> getDataSources() {
        return dataSources;
    }

    public void setDataSources(List<MogDataSource> dataSources) {
        this.dataSources.clear();
        this.dataSources.addAll(dataSources);
    }

    public MogContext getContext() {
        return context;
    }

    public void setContext(MogContext context) {
        this.context = context;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((sheets == null) ? 0 : sheets.hashCode());
        result = prime * result + ((dataSources == null) ? 0 : dataSources.hashCode());
        result = prime * result + ((filename == null) ? 0 : filename.hashCode());
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
        Report other = (Report) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (sheets == null) {
            if (other.sheets != null)
                return false;
        } else if (!sheets.equals(other.sheets))
            return false;
        if (dataSources == null) {
            if (other.dataSources != null)
                return false;
        } else if (!dataSources.equals(other.dataSources))
            return false;
        if (filename == null) {
            if (other.filename != null)
                return false;
        } else if (!filename.equals(other.filename))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Report [name=" + name + ", type=" + type + ", sheets=" + sheets + ", dataSources=" + dataSources
                + ", filename=" + filename + "]";
    }
   
}
