package org.copperforge.mog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.copperforge.mog.io.MogFileFinder;
import org.copperforge.mog.reader.MogReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MogFinder<T, R extends MogReader<T>> {

    private MogReader<T> reader;
    private Logger log = LoggerFactory.getLogger(MogFinder.class);

    public MogFinder(R reader) throws MogException {
        this.reader = reader;
    }

    public List<T> find(String mogPath, String mogPattern) throws MogException {
        List<T> mogs = new ArrayList<>();

        List<File> files = new MogFileFinder().listFiles(mogPath, mogPattern);
        for (File file : files) {
            log.info("Found file '" + file.getAbsolutePath() + "'");
            mogs.add(reader.read(file));
        }

        return mogs;
    }
}
