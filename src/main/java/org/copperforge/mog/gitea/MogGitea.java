package org.copperforge.mog.gitea;

import org.copperforge.mog.git.MogGitConfig;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MogGitea extends MogGitConfig {

    @JsonProperty("siteUrl")
    @JsonAlias("site")
    private String siteUrl;

    @JsonProperty("apiUrl")
    @JsonAlias("api")
    private String apiUrl;

    private String token;

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "MogGiteaConfig [siteUrl=" + siteUrl + ", apiUrl=" + apiUrl + ", token=" + token + ", getType()="
                + getType() + ", getName()=" + getName() + "]";
    }

}
