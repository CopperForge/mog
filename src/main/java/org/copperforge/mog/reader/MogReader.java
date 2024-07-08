package org.copperforge.mog.reader;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;

import org.copperforge.mog.MogException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MogReader<T> {

    private final Class<T> type;
    private static final ObjectMapper _mapper = new ObjectMapper();

    public MogReader(Class<T> type) {
        this.type = type;
    }

    public T read(final String filename) throws MogException {
        try {
            return read(new FileInputStream(filename));
        } catch (Exception e) {
            throw new MogException("Unable to load MOG file :: " + filename, e);
        }
    }

    public T parse(final String text) throws MogException {
        try {
            return read(new ByteArrayInputStream(text.getBytes()));
        } catch (Exception e) {
            throw new MogException("Unable to load string :: " + text, e);
        }
    }

    protected T read(final InputStream stream) throws Exception {
        T obj = _mapper.readValue(stream, type);
        return obj;
    }

}
