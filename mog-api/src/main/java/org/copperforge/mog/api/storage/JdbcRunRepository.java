package org.copperforge.mog.api.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.copperforge.mog.api.config.MogApiProperties;
import org.copperforge.mog.api.run.RunMetadata;
import org.copperforge.mog.api.run.RunStatus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JdbcRunRepository implements RunRepository {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final FileSystemStorageLayout layout;
    private final MogApiProperties properties;
    private final ObjectMapper objectMapper;

    public JdbcRunRepository(FileSystemStorageLayout layout, MogApiProperties properties, ObjectMapper objectMapper)
            throws IOException {
        this.layout = layout;
        this.properties = properties;
        this.objectMapper = objectMapper;
        initializeSchema();
    }

    @Override
    public Path createRunDirectory(String runId) throws IOException {
        Path dir = layout.runDirectory(runId);
        Files.createDirectories(dir);
        return dir;
    }

    @Override
    public Path runDirectory(String runId) {
        return layout.runDirectory(runId);
    }

    @Override
    public void saveMetadata(RunMetadata metadata) throws IOException {
        try (Connection connection = openConnection()) {
            int updated = update(connection, metadata);
            if (updated == 0) {
                insert(connection, metadata);
            }
        } catch (SQLException ex) {
            throw new IOException("Unable to save run metadata", ex);
        }
    }

    @Override
    public RunMetadata loadMetadata(String runId) throws IOException {
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        select run_id, report_id, datasource_id, params_json, format, status,
                               started_at, completed_at, message,
                               artifact_file_name, artifact_content_type, artifact_size
                        from runs
                        where run_id = ?
                        """)) {
            statement.setString(1, runId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NoSuchFileException("Run '" + runId + "' not found");
                }
                return mapMetadata(rs);
            }
        } catch (SQLException ex) {
            throw new IOException("Unable to load run metadata", ex);
        }
    }

    @Override
    public List<RunMetadata> listMetadata() throws IOException {
        List<RunMetadata> metadata = new ArrayList<>();
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        select run_id, report_id, datasource_id, params_json, format, status,
                               started_at, completed_at, message,
                               artifact_file_name, artifact_content_type, artifact_size
                        from runs
                        order by started_at desc nulls last, run_id desc
                        """)) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    metadata.add(mapMetadata(rs));
                }
            }
            return metadata;
        } catch (SQLException ex) {
            throw new IOException("Unable to list run metadata", ex);
        }
    }

    private void initializeSchema() throws IOException {
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        create table if not exists runs (
                            run_id varchar(100) primary key,
                            report_id varchar(255) not null,
                            datasource_id varchar(255) not null,
                            params_json clob,
                            format varchar(50),
                            status varchar(50),
                            started_at timestamp,
                            completed_at timestamp,
                            message clob,
                            artifact_file_name varchar(512),
                            artifact_content_type varchar(255),
                            artifact_size bigint
                        )
                        """)) {
            statement.execute();
        } catch (SQLException ex) {
            throw new IOException("Unable to initialize run metadata schema", ex);
        }
    }

    private Connection openConnection() throws SQLException {
        String url = properties.resolvedRunMetadataJdbcUrl();
        String user = properties.getRunMetadataStore().getJdbcUser();
        String password = properties.getRunMetadataStore().getJdbcPassword();
        if (user != null && !user.isBlank()) {
            return DriverManager.getConnection(url, user, password != null ? password : "");
        }
        return DriverManager.getConnection(url);
    }

    private int update(Connection connection, RunMetadata metadata) throws SQLException, IOException {
        try (PreparedStatement statement = connection.prepareStatement("""
                update runs
                   set report_id = ?, datasource_id = ?, params_json = ?, format = ?, status = ?,
                       started_at = ?, completed_at = ?, message = ?,
                       artifact_file_name = ?, artifact_content_type = ?, artifact_size = ?
                 where run_id = ?
                """)) {
            bindMetadata(statement, metadata, false);
            statement.setString(12, metadata.getRunId());
            return statement.executeUpdate();
        }
    }

    private void insert(Connection connection, RunMetadata metadata) throws SQLException, IOException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into runs (
                    report_id, datasource_id, params_json, format, status,
                    started_at, completed_at, message,
                    artifact_file_name, artifact_content_type, artifact_size, run_id
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            bindMetadata(statement, metadata, false);
            statement.setString(12, metadata.getRunId());
            statement.executeUpdate();
        }
    }

    private void bindMetadata(PreparedStatement statement, RunMetadata metadata, boolean includeRunIdFirst)
            throws SQLException, IOException {
        int index = includeRunIdFirst ? 2 : 1;
        if (includeRunIdFirst) {
            statement.setString(1, metadata.getRunId());
        }
        statement.setString(index++, metadata.getReportId());
        statement.setString(index++, metadata.getDatasourceId());
        statement.setString(index++, objectMapper.writeValueAsString(metadata.getParams() != null ? metadata.getParams() : Map.of()));
        statement.setString(index++, metadata.getFormat());
        statement.setString(index++, metadata.getStatus() != null ? metadata.getStatus().name() : null);
        setTimestamp(statement, index++, metadata.getStartedAt());
        setTimestamp(statement, index++, metadata.getCompletedAt());
        statement.setString(index++, metadata.getMessage());
        RunMetadata.ArtifactMetadata artifact = metadata.getArtifact();
        if (artifact != null) {
            statement.setString(index++, artifact.getFileName());
            statement.setString(index++, artifact.getContentType());
            statement.setLong(index++, artifact.getSize());
        } else {
            statement.setNull(index++, Types.VARCHAR);
            statement.setNull(index++, Types.VARCHAR);
            statement.setNull(index++, Types.BIGINT);
        }
    }

    private void setTimestamp(PreparedStatement statement, int index, Instant instant) throws SQLException {
        if (instant == null) {
            statement.setNull(index, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index, Timestamp.from(instant));
        }
    }

    private RunMetadata mapMetadata(ResultSet rs) throws SQLException, IOException {
        RunMetadata metadata = new RunMetadata();
        metadata.setRunId(rs.getString("run_id"));
        metadata.setReportId(rs.getString("report_id"));
        metadata.setDatasourceId(rs.getString("datasource_id"));
        String paramsJson = rs.getString("params_json");
        if (paramsJson != null && !paramsJson.isBlank()) {
            metadata.setParams(objectMapper.readValue(paramsJson, MAP_TYPE));
        }
        metadata.setFormat(rs.getString("format"));
        String status = rs.getString("status");
        if (status != null && !status.isBlank()) {
            metadata.setStatus(RunStatus.valueOf(status));
        }
        Timestamp startedAt = rs.getTimestamp("started_at");
        if (startedAt != null) {
            metadata.setStartedAt(startedAt.toInstant());
        }
        Timestamp completedAt = rs.getTimestamp("completed_at");
        if (completedAt != null) {
            metadata.setCompletedAt(completedAt.toInstant());
        }
        metadata.setMessage(rs.getString("message"));
        String artifactFileName = rs.getString("artifact_file_name");
        if (artifactFileName != null && !artifactFileName.isBlank()) {
            long artifactSize = rs.getLong("artifact_size");
            if (rs.wasNull()) {
                artifactSize = 0L;
            }
            metadata.setArtifact(new RunMetadata.ArtifactMetadata(
                    artifactFileName,
                    rs.getString("artifact_content_type"),
                    artifactSize));
        }
        return metadata;
    }
}
