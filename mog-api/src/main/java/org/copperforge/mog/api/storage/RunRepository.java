package org.copperforge.mog.api.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.copperforge.mog.api.run.RunMetadata;

public interface RunRepository {

    Path createRunDirectory(String runId) throws IOException;

    Path runDirectory(String runId);

    void saveMetadata(RunMetadata metadata) throws IOException;

    RunMetadata loadMetadata(String runId) throws IOException;

    List<RunMetadata> listMetadata() throws IOException;
}
