package org.copperforge.mog.reader;

import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;

import org.copperforge.mog.MogException;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class MogReader<T> {

    private final Class<T> type;
    private final ObjectMapper mapper;

    public MogReader(Class<T> type) {
        this.type = type;

        mapper = JsonMapper.builder().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS).build();
    }

    public T read(final String filename) throws MogException {
        try {
            return read(new FileInputStream(filename));
        } catch (Exception e) {
            throw new MogException("Unable to load MOG file :: " + filename, e);
        }
    }

    public T read(final File file) throws MogException {
        try {
            return read(new FileInputStream(file));
        } catch (Exception e) {
            throw new MogException("Unable to load MOG file :: " + file.getAbsolutePath(), e);
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
        T obj = mapper.readValue(stream, type);
        return obj;
    }

}
