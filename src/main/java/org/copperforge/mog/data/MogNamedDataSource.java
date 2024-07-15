package org.copperforge.mog.data;

import java.util.List;

import org.copperforge.mog.MogException;

public class MogNamedDataSource extends MogDataSource {

    private String name;

    @Override
    public List<MogFetchable> fetch() throws MogException {
        throw new MogException("Unimplemented method 'data'");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
