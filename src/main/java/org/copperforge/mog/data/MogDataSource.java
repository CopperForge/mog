package org.copperforge.mog.data;

import java.util.List;

import org.copperforge.mog.MogException;
import org.copperforge.mog.data.dotout.MogDotOutDataSource;
import org.copperforge.mog.data.filter.MogDataFilter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MogJdbcDataSource.class, name = "jdbc"),
        @JsonSubTypes.Type(value = MogRestDataSource.class, name = "api"),
        @JsonSubTypes.Type(value = MogRestDataSource.class, name = "rest"),
        @JsonSubTypes.Type(value = MogDotOutDataSource.class, name = "dotout"),
        @JsonSubTypes.Type(value = MogDotOutDataSource.class, name = "binary")
})
public abstract class MogDataSource {

    private String name;
    private String type;

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

    public abstract List<? extends MogFetchable> fetch(MogDataFilter filter) throws MogException;

    @Override
    public String toString() {
        return "MogDataSource [name=" + name + ", type=" + type + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        MogDataSource other = (MogDataSource) obj;
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
        return true;
    }


}
