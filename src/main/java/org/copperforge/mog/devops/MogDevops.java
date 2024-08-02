package org.copperforge.mog.devops;

import java.util.ArrayList;
import java.util.List;

import org.copperforge.mog.MogObject;
import org.copperforge.mog.scm.MogScm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MogDevops implements MogObject {

    @JsonProperty("scms")
    @JsonAlias("sourceControl")
    private List<MogScm> scms = new ArrayList<>();

    public List<MogScm> getScms() {
        return scms;
    }

    public void setScms(List<MogScm> scms) {
        this.scms = scms;
    }

    @Override
    public String toString() {
        return "MogDevops [scms=" + scms + "]";
    }
    
}
