package org.copperforge.mog.gitea;

import org.copperforge.mog.git.MogGitConfig;

public class MogGitea extends MogGitConfig {

    private String siteUrl;

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
