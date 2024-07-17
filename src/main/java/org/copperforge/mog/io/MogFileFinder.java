package org.copperforge.mog.io;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

import org.copperforge.mog.MogException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MogFileFinder {

    private Logger log = LoggerFactory.getLogger(MogFileFinder.class);

    public List<File> listFiles(String absolutePath) {
        List<File> files = new ArrayList<>();
        File d = new File(absolutePath);
        for (File f : d.listFiles()) {
            if (f.isDirectory()) {
                files.addAll(listFiles(f.getAbsolutePath()));
            } else {
                files.add(f);
            }
        }
        return files;
    }

    public List<File> listFiles(String base, String pattern) throws MogException {
        return new MogWildcardMatchFileVisitor(pattern).find(base);
    }

    public List<File> matchFiles(List<File> files, String pattern) throws MogException {
        List<File> matches = new ArrayList<>();
        PathMatcher matcher;
        for (File asset : files) {
            matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            if (matcher.matches(asset.toPath())) {
                log.info("File '" + asset.getAbsolutePath() + "' matches pattern '" + pattern + "'");
                matches.add(asset);
            }
        }
        return matches;
    }

}
