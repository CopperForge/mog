package org.copperforge.mog.data;

import java.util.List;

import org.copperforge.mog.MogException;

public interface MogFetchCursor extends AutoCloseable {

    List<String> columns() throws MogException;

    boolean next() throws MogException;

    MogFetchable current() throws MogException;

    long rowNumber();

    @Override
    void close() throws MogException;
}
