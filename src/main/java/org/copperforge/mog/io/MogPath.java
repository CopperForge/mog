package org.copperforge.mog.io;

public class MogPath {

    private String fullPath;
    private String base;
    private String filename;

    public MogPath(String fullpath) {
        this.fullPath = fullpath;

        int lastIndex = 0;
        if ((fullpath.contains("/") && fullpath.contains("\\"))) {
            if (fullpath.lastIndexOf("/") > fullpath.lastIndexOf("\\")) {
                lastIndex = fullpath.lastIndexOf("/");
            } else {
                lastIndex = fullpath.lastIndexOf("\\");
            }
        } else if (fullpath.contains("/")) {
            lastIndex = fullpath.lastIndexOf("/");
        } else if (fullpath.contains("\\")) {
            lastIndex = fullpath.lastIndexOf("\\");
        }

        this.base = fullpath.substring(0, lastIndex);
        this.filename = fullpath.substring(lastIndex + 1);        
    }

    public String getFullPath() {
        return fullPath;
    }

    public String getBase() {
        return base;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fullPath == null) ? 0 : fullPath.hashCode());
        result = prime * result + ((base == null) ? 0 : base.hashCode());
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
        MogPath other = (MogPath) obj;
        if (fullPath == null) {
            if (other.fullPath != null)
                return false;
        } else if (!fullPath.equals(other.fullPath))
            return false;
        if (base == null) {
            if (other.base != null)
                return false;
        } else if (!base.equals(other.base))
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
        return "MogPath [fullPath=" + fullPath + ", base=" + base + ", filename=" + filename + "]";
    }

}
