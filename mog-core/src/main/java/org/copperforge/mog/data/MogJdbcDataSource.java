package org.copperforge.mog.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.copperforge.mog.MogException;
import org.copperforge.mog.data.filter.MogDataFilter;
import org.copperforge.mog.data.filter.MogQueryFilter;
import org.copperforge.mog.data.sql.MogNamedSqlParser;
import org.copperforge.mog.data.sql.MogPreparedSql;
import org.copperforge.mog.data.sql.MogPreparedStatementBinder;
import org.copperforge.mog.data.sql.MogResolvedSqlParameter;
import org.copperforge.mog.data.sql.MogSqlParameterResolver;
import org.copperforge.mog.runtime.MogContext;
import org.copperforge.mog.security.MogSecurityService;
import org.copperforge.mog.var.MogVariableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MogJdbcDataSource extends MogDataSource {

    private Logger log = LoggerFactory.getLogger(MogJdbcDataSource.class);

    private String url;

    private String user;

    private String password;

    private String jdbcClass;

    private Integer fetchSize;

    private Integer queryTimeoutSeconds;

    private Boolean readOnly;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getJdbcClass() {
        return jdbcClass;
    }

    public void setJdbcClass(String jdbcClass) {
        this.jdbcClass = jdbcClass;
    }

    public Integer getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(Integer fetchSize) {
        this.fetchSize = fetchSize;
    }

    public Integer getQueryTimeoutSeconds() {
        return queryTimeoutSeconds;
    }

    public void setQueryTimeoutSeconds(Integer queryTimeoutSeconds) {
        this.queryTimeoutSeconds = queryTimeoutSeconds;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public String toString() {
        return "MogJdbcDataSource [url=" + url + ", user=" + user + ", password=*, jdbcClass=" + jdbcClass
                + ", fetchSize=" + fetchSize + ", queryTimeoutSeconds=" + queryTimeoutSeconds
                + ", readOnly=" + readOnly + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((jdbcClass == null) ? 0 : jdbcClass.hashCode());
        result = prime * result + ((fetchSize == null) ? 0 : fetchSize.hashCode());
        result = prime * result + ((queryTimeoutSeconds == null) ? 0 : queryTimeoutSeconds.hashCode());
        result = prime * result + ((readOnly == null) ? 0 : readOnly.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MogJdbcDataSource other = (MogJdbcDataSource) obj;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        if (user == null) {
            if (other.user != null)
                return false;
        } else if (!user.equals(other.user))
            return false;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (jdbcClass == null) {
            if (other.jdbcClass != null)
                return false;
        } else if (!jdbcClass.equals(other.jdbcClass))
            return false;
        if (fetchSize == null) {
            if (other.fetchSize != null)
                return false;
        } else if (!fetchSize.equals(other.fetchSize))
            return false;
        if (queryTimeoutSeconds == null) {
            if (other.queryTimeoutSeconds != null)
                return false;
        } else if (!queryTimeoutSeconds.equals(other.queryTimeoutSeconds))
            return false;
        if (readOnly == null) {
            if (other.readOnly != null)
                return false;
        } else if (!readOnly.equals(other.readOnly))
            return false;
        return true;
    }

    @Override
    public List<MogFetchable> fetch(MogDataFilter filter, MogContext context) throws MogException {
        log.trace("Fetching using " + filter);
        List<MogFetchable> data = new ArrayList<>();
        try (MogFetchCursor cursor = openCursor(filter, context)) {
            while (cursor.next()) {
                data.add(cursor.current());
            }
        }
        return data;
    }

    @Override
    public MogFetchCursor openCursor(MogDataFilter filter, MogContext context) throws MogException {
        log.trace("Opening JDBC cursor using " + filter);
        MogQueryFilter queryFilter = requireFilter(filter, MogQueryFilter.class, "query");

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            MogVariableService variables = new MogVariableService(context);
            String url = variables.envsubst(getUrl()); // url
            String username = variables.envsubst(getUser()); // credentials
            String password = "";
            String configuredPassword = variables.envsubst(getPassword());
            if (configuredPassword != null && !configuredPassword.isBlank()) {
                MogSecurityService securityService = new MogSecurityService(context, null);
                password = securityService.decryptor().decrypt(configuredPassword);
            }
            String query = queryFilter.getQuery(); // query to be run
            if (getJdbcClass() != null && !getJdbcClass().isEmpty())
                Class.forName(getJdbcClass()); // Driver name

            if (filter.getOffset() > 0) {
                query = addOffset(query, filter.getOffset().intValue());
            }

            if (filter.getLimit() > 0) {
                query = addLimit(query, filter.getLimit().intValue());
            }

            MogNamedSqlParser sqlParser = new MogNamedSqlParser();
            MogPreparedSql preparedSql = sqlParser.parse(query);
            List<MogResolvedSqlParameter> parameters = new MogSqlParameterResolver()
                    .resolve(preparedSql, queryFilter.getParameters(), context);

            connection = DriverManager.getConnection(url, username, password);
            if (getReadOnly() != null) {
                connection.setReadOnly(getReadOnly());
            }
            statement = connection.prepareStatement(preparedSql.sql(), ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY);
            if (getFetchSize() != null) {
                statement.setFetchSize(getFetchSize());
            }
            if (getQueryTimeoutSeconds() != null) {
                statement.setQueryTimeout(getQueryTimeoutSeconds());
            }
            new MogPreparedStatementBinder().bind(statement, parameters);
            resultSet = statement.executeQuery();

            MogJdbcFetchCursor cursor = new MogJdbcFetchCursor(resultSet, statement, connection);
            resultSet = null;
            statement = null;
            connection = null;
            return cursor;
        } catch (MogException e) {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
            throw e;
        } catch (Exception e) {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
            throw new MogException(e);
        }
    }

    private String addLimit(String query, int limit) {
        return query + " FETCH NEXT " + limit + " ROWS ONLY";
    }

    private String addOffset(String query, int offset) {
        return query + " OFFSET " + offset + " ROWS";
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignore) {
        }
    }

    private static final class MogJdbcFetchCursor implements MogFetchCursor {
        private final ResultSet resultSet;
        private final PreparedStatement statement;
        private final Connection connection;
        private final ResultSetMetaData metadata;
        private final List<String> columns;
        private MogFetchable current;
        private long rowNumber = 0;
        private boolean closed = false;
        private boolean exhausted = false;

        private MogJdbcFetchCursor(ResultSet resultSet, PreparedStatement statement, Connection connection)
                throws SQLException {
            this.resultSet = resultSet;
            this.statement = statement;
            this.connection = connection;
            this.metadata = resultSet.getMetaData();
            this.columns = resolveColumns(metadata);
        }

        @Override
        public List<String> columns() throws MogException {
            ensureOpen();
            return columns;
        }

        @Override
        public boolean next() throws MogException {
            ensureOpen();
            if (exhausted) {
                current = null;
                return false;
            }
            try {
                if (!resultSet.next()) {
                    current = null;
                    exhausted = true;
                    return false;
                }
                current = toFetchable(resultSet, metadata);
                rowNumber++;
                return true;
            } catch (SQLException e) {
                current = null;
                throw new MogException(e);
            }
        }

        @Override
        public MogFetchable current() throws MogException {
            ensureOpen();
            if (current == null) {
                throw new MogException("Cursor is not positioned on a row");
            }
            return current;
        }

        @Override
        public long rowNumber() {
            return rowNumber;
        }

        @Override
        public void close() throws MogException {
            if (closed) {
                return;
            }
            closed = true;
            current = null;

            MogException failure = null;
            failure = close(resultSet, failure);
            failure = close(statement, failure);
            failure = close(connection, failure);
            if (failure != null) {
                throw failure;
            }
        }

        private void ensureOpen() throws MogException {
            if (closed) {
                throw new MogException("Cursor is closed");
            }
        }

        private static List<String> resolveColumns(ResultSetMetaData metadata) throws SQLException {
            List<String> names = new ArrayList<>();
            for (int colidx = 1; colidx <= metadata.getColumnCount(); colidx++) {
                names.add(metadata.getColumnName(colidx).toLowerCase());
            }
            return List.copyOf(names);
        }

        private static MogFetchable toFetchable(ResultSet resultSet, ResultSetMetaData metadata)
                throws SQLException {
            MogFetchable reportable = new MogFetchable();
            for (int colidx = 1; colidx <= metadata.getColumnCount(); colidx++) {
                String columnName = metadata.getColumnName(colidx);
                Object value = resultSet.getObject(colidx);
                reportable.set(columnName.toLowerCase(), value);
            }
            return reportable;
        }

        private static MogException close(AutoCloseable closeable, MogException existing) {
            if (closeable == null) {
                return existing;
            }
            try {
                closeable.close();
                return existing;
            } catch (Exception e) {
                MogException failure = new MogException(e);
                if (existing != null) {
                    existing.addSuppressed(e);
                    return existing;
                }
                return failure;
            }
        }
    }
}
