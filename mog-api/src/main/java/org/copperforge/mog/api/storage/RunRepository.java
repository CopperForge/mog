package org.copperforge.mog.api.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface RunRepository {

    Path createRunDirectory(String runId) throws IOException;

    Path runDirectory(String runId);

    Path metadataFile(String runId);

    List<String> listRunIds() throws IOException;
}
