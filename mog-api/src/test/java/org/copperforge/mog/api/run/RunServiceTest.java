package org.copperforge.mog.api.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.copperforge.mog.api.config.MogApiProperties;
import org.copperforge.mog.api.storage.DslStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

class RunServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void execute_usesRunParamsForInlineDatasource_andKeepsInlineDatasourcePrecedence() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        MogApiProperties properties = new MogApiProperties();
        properties.setStoreDir(tempDir.resolve("store"));
        properties.setMogHome(tempDir.toString());
        properties.setMogEtc(tempDir.resolve("etc").toString());

        DslStorageService storage = new DslStorageService(objectMapper, properties);
        RunService runService = new RunService(storage, properties, objectMapper);

        Path inlineJson = tempDir.resolve("inline.json");
        Files.writeString(inlineJson, "{\"data\":[{\"message\":\"INLINE\"}]}");

        Path externalJson = tempDir.resolve("external.json");
        Files.writeString(externalJson, "{\"data\":[{\"message\":\"EXTERNAL\"}]}");

        storage.saveReport(objectMapper.readTree("""
                {
                  "id": "report-inline-wins",
                  "name": "report-inline-wins",
                  "type": "xlsx",
                  "filename": "ignored.xlsx",
                  "dataSources": [
                    { "name": "shared", "type": "json", "file": "${inline_file}" }
                  ],
                  "sheets": [
                    {
                      "name": "Sheet1",
                      "title": "Sheet1",
                      "elements": [
                        {
                          "type": "table",
                          "name": "t_messages",
                          "title": "Messages",
                          "upperLeft": { "row": 1, "col": 1 },
                          "columns": [
                            { "title": "Message", "key": "message" }
                          ],
                          "dataSource": {
                            "name": "shared",
                            "filter": { "type": "json", "jsonPath": "$.data[*]" }
                          }
                        }
                      ]
                    }
                  ]
                }
                """));

        storage.saveDatasource(objectMapper.readTree("""
                {
                  "id": "datasource-inline-wins",
                  "datasources": [
                    { "name": "shared", "type": "json", "file": "%s" }
                  ]
                }
                """.formatted(escapeForJson(externalJson))));

        RunRequest request = new RunRequest(
                "report-inline-wins",
                "datasource-inline-wins",
                Map.of("inline_file", inlineJson.toString()),
                new RunRequest.RunOutput("xlsx"));

        RunMetadata metadata = runService.execute(request);

        assertNotNull(metadata.getRunId());
        assertEquals("artifact.xlsx", metadata.getArtifact().getFileName());
        assertEquals("COMPLETED", metadata.getStatus().name());
        assertEquals(inlineJson.toString(), metadata.getParams().get("inline_file"));

        Path artifact = storage.runDirectory(metadata.getRunId()).resolve(metadata.getArtifact().getFileName());
        try (XSSFWorkbook workbook = new XSSFWorkbook(artifact.toFile())) {
            assertEquals("Message", workbook.getSheet("Sheet1").getRow(0).getCell(0).getStringCellValue());
            assertEquals("INLINE", workbook.getSheet("Sheet1").getRow(1).getCell(0).getStringCellValue());
        }
    }

    private String escapeForJson(Path path) {
        return path.toString().replace("\\", "\\\\");
    }
}
