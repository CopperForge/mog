package org.copperforge.mog.data;

import java.util.List;

import org.copperforge.mog.MogException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = MogJdbcDataSource.class, name = "jdbc"),
    @JsonSubTypes.Type(value = MogRestDataSource.class, name = "api"),
    @JsonSubTypes.Type(value = MogRestDataSource.class, name = "rest")
})
public abstract class MogDataSource {

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public abstract List<Fetchable> data() throws MogException;

}
