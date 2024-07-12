package org.copperforge.mog.reporting.datasource;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.copperforge.mog.reporting.core.Reportable;
import org.copperforge.mog.reporting.datasource.dotout.ReportDotOutDataSource;
import org.copperforge.mog.reporting.datasource.jdbc.ReportJdbcDataSource;
import org.copperforge.mog.reporting.datasource.rest.ReportRestDataSource;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ReportJdbcDataSource.class, name = "jdbc"),
    @JsonSubTypes.Type(value = ReportDotOutDataSource.class, name = "dotout"),
    @JsonSubTypes.Type(value = ReportRestDataSource.class, name = "api")
})
public abstract class ReportDataSource {

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public abstract List<Reportable> data();

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ReportDataSource [type=" + type + "]";
    }

    
}
