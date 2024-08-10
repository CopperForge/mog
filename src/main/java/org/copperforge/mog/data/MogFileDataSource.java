package org.copperforge.mog.data;

public abstract class MogFileDataSource extends MogDataSource {

    private String file;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public String toString() {
        return "MogFileDataSource [file=" + file + ", getName()=" + getName() + ", getType()=" + getType() + "]";
    }

}
