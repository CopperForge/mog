package org.copperforge.mog.gitea.models;

import org.copperforge.mog.MogBean;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GiteaRepository extends MogBean {

    private String name;

    private String url;

    @JsonProperty("ssh_url")
    private String sshCloneUrl;

    @JsonProperty("clone_url")
    private String cloneUrl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSshCloneUrl() {
        return sshCloneUrl;
    }

    public void setSshCloneUrl(String ssh_url) {
        this.sshCloneUrl = ssh_url;
    }

    @Override
    public String toString() {
        return "GiteaRepository [name=" + name + ", url=" + url + ", ssh_url=" + sshCloneUrl + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        result = prime * result + ((sshCloneUrl == null) ? 0 : sshCloneUrl.hashCode());
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
        GiteaRepository other = (GiteaRepository) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        if (sshCloneUrl == null) {
            if (other.sshCloneUrl != null)
                return false;
        } else if (!sshCloneUrl.equals(other.sshCloneUrl))
            return false;
        return true;
    }

    public String getCloneUrl() {
        return cloneUrl;
    }

    public void setCloneUrl(String cloneUrl) {
        this.cloneUrl = cloneUrl;
    }

}
