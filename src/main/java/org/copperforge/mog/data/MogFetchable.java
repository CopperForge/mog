package org.copperforge.mog.data;

import java.util.Map;

import org.copperforge.mog.MogBean;
import org.copperforge.mog.MogException;

public class MogFetchable extends MogBean {

    public MogFetchable() {
        super();
    }

    public MogFetchable(Map<String, Object> fields) {
        super(fields);
    }

    public MogFetchable(Object obj) throws MogException {
        super(obj);
    }

}
