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
        @JsonSubTypes.Type(value = MogRestDataSource.class, name = "rest"),
        @JsonSubTypes.Type(value = MogNamedDataSource.class, name = "named")
})
public abstract class MogDataSource {

    private String type;
    private Integer offset = 0;
    private Integer limit = -1;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public MogDataSource offset(int offset) {
        this.offset = offset;
        return this;
    }

    public MogDataSource limit(int limit) {
        this.limit = limit;
        return this;
    }

    public List<MogFetchable> fetch() throws MogException {
        return fetch(this.offset, this.limit);
    }

    public abstract List<MogFetchable> fetch(int offset, int limit) throws MogException;

}
