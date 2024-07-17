package org.copperforge.mog.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.copperforge.mog.MogException;

public class MogWildcardMatchFileVisitor extends SimpleFileVisitor<Path> {

    private final List<File> matches = new ArrayList<>();
    private String pattern;

    public MogWildcardMatchFileVisitor(String pattern) {
        this.pattern = pattern;
        matches.clear();
    }

    public List<File> find(final String path) throws MogException {
        try {
            Files.walkFileTree(new File(path).toPath(), this);
            return matches;
        } catch (IOException e) {
            throw new MogException(e);
        }
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attribs) throws IOException {
        FileSystem fs = FileSystems.getDefault();
        PathMatcher matcher = fs.getPathMatcher("glob:" + pattern);
        Path name = file.getFileName();
        if (matcher.matches(name)) {
            matches.add(file.toFile());
        }
        return FileVisitResult.CONTINUE;
    }
}
