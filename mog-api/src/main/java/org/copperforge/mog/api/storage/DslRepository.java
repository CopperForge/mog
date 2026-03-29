package org.copperforge.mog.api.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public interface DslRepository {

    SaveResult saveDatasource(JsonNode json) throws IOException;

    SaveResult saveReport(JsonNode json) throws IOException;

    Path requireDatasource(String id) throws IOException;

    Path requireReport(String id) throws IOException;

    List<String> listDatasourceIds() throws IOException;

    List<String> listReportIds() throws IOException;

    JsonNode loadDatasource(String id) throws IOException;

    JsonNode loadReport(String id) throws IOException;

    record SaveResult(String id, Path file) { }
}
