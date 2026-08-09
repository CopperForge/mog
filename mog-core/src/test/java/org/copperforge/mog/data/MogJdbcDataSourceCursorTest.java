package org.copperforge.mog.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.copperforge.mog.MogException;
import org.copperforge.mog.data.filter.MogDataFilter;
import org.copperforge.mog.data.filter.MogQueryFilter;
import org.copperforge.mog.data.filter.MogQueryParameter;
import org.copperforge.mog.runtime.MogContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MogJdbcDataSourceCursorTest {

    @BeforeAll
    static void registerProxyDriver() throws Exception {
        ProxyDriver.register();
    }

    @Test
    void openCursor_executesUnparameterizedQueryThroughPreparedStatement() throws Exception {
        Recording recording = new Recording("jdbc:h2:mem:mog-cursor-unparameterized;DB_CLOSE_DELAY=-1");
        MogJdbcDataSource dataSource = proxyDataSource(recording);

        try (MogFetchCursor cursor = dataSource.openCursor(new MogQueryFilter("select 1 as id"), context())) {
            assertEquals(List.of("id"), cursor.columns());
            assertTrue(cursor.next());
            assertEquals(1, ((Number) cursor.current().get("id")).intValue());
            assertFalse(cursor.next());
        }

        assertEquals("select 1 as id", recording.preparedSql);
        assertEquals(ResultSet.TYPE_FORWARD_ONLY, recording.resultSetType);
        assertEquals(ResultSet.CONCUR_READ_ONLY, recording.resultSetConcurrency);
    }

    @Test
    void openCursor_doesNotUseFetchCompatibilityPath() throws Exception {
        MogJdbcDataSource dataSource = new MogJdbcDataSource() {
            @Override
            public List<MogFetchable> fetch(MogDataFilter filter, MogContext context) throws MogException {
                throw new MogException("fetch must not be called");
            }
        };
        dataSource.setUrl("jdbc:h2:mem:mog-cursor-no-fetch;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword(null);

        try (MogFetchCursor cursor = dataSource.openCursor(new MogQueryFilter("select 1 as id"), context())) {
            assertTrue(cursor.next());
            assertEquals(1, ((Number) cursor.current().get("id")).intValue());
        }
    }

    @Test
    void openCursor_canIterateLargeGeneratedResultSetWithoutRetainedCollection() throws Exception {
        MogJdbcDataSource dataSource = h2DataSource("jdbc:h2:mem:mog-cursor-large;DB_CLOSE_DELAY=-1");

        long count = 0;
        try (MogFetchCursor cursor = dataSource.openCursor(
                new MogQueryFilter("select x as id from system_range(1, 100000)"), context())) {
            while (cursor.next()) {
                count++;
                if (count % 25000 == 0) {
                    assertEquals(count, ((Number) cursor.current().get("x")).longValue());
                }
            }
            assertEquals(100000, cursor.rowNumber());
        }

        assertEquals(100000, count);
    }

    @Test
    void columns_workBeforeNextAndZeroRowsExposeMetadata() throws Exception {
        MogJdbcDataSource dataSource = h2DataSource("jdbc:h2:mem:mog-cursor-columns;DB_CLOSE_DELAY=-1");

        try (MogFetchCursor cursor = dataSource.openCursor(new MogQueryFilter(
                "select cast(null as int) as Id, cast(null as varchar) as Name where 1 = 0"), context())) {
            assertEquals(List.of("id", "name"), cursor.columns());
            assertEquals(0, cursor.rowNumber());
            assertFalse(cursor.next());
            assertEquals(0, cursor.rowNumber());
        }
    }

    @Test
    void close_closesResultSetPreparedStatementAndConnectionAndIsIdempotent() throws Exception {
        Recording recording = new Recording("jdbc:h2:mem:mog-cursor-close;DB_CLOSE_DELAY=-1");
        MogJdbcDataSource dataSource = proxyDataSource(recording);

        MogFetchCursor cursor = dataSource.openCursor(new MogQueryFilter("select 1 as id"), context());
        assertTrue(cursor.next());
        cursor.close();
        cursor.close();

        assertEquals(1, recording.resultSetCloseCount);
        assertEquals(1, recording.preparedStatementCloseCount);
        assertEquals(1, recording.connectionCloseCount);
    }

    @Test
    void openFailure_closesPartialResources() throws Exception {
        Recording recording = new Recording("jdbc:h2:mem:mog-cursor-open-failure;DB_CLOSE_DELAY=-1");
        recording.throwOnExecuteQuery = true;
        MogJdbcDataSource dataSource = proxyDataSource(recording);

        assertThrows(MogException.class,
                () -> dataSource.openCursor(new MogQueryFilter("select 1 as id"), context()));

        assertEquals(0, recording.resultSetCloseCount);
        assertEquals(1, recording.preparedStatementCloseCount);
        assertEquals(1, recording.connectionCloseCount);
    }

    @Test
    void consumerFailureFollowedByClose_cleansResources() throws Exception {
        Recording recording = new Recording("jdbc:h2:mem:mog-cursor-consumer-failure;DB_CLOSE_DELAY=-1");
        MogJdbcDataSource dataSource = proxyDataSource(recording);

        MogFetchCursor cursor = dataSource.openCursor(new MogQueryFilter("select 1 as id"), context());
        assertTrue(cursor.next());
        assertThrows(IllegalStateException.class, () -> {
            throw new IllegalStateException("consumer failed");
        });
        cursor.close();

        assertEquals(1, recording.resultSetCloseCount);
        assertEquals(1, recording.preparedStatementCloseCount);
        assertEquals(1, recording.connectionCloseCount);
    }

    @Test
    void sqlExceptionDuringNext_surfacesAsMogExceptionAndRemainsCloseable() throws Exception {
        Recording recording = new Recording("jdbc:h2:mem:mog-cursor-next-failure;DB_CLOSE_DELAY=-1");
        recording.throwOnNext = true;
        MogJdbcDataSource dataSource = proxyDataSource(recording);

        MogFetchCursor cursor = dataSource.openCursor(new MogQueryFilter("select 1 as id"), context());
        assertThrows(MogException.class, cursor::next);
        cursor.close();

        assertEquals(1, recording.resultSetCloseCount);
        assertEquals(1, recording.preparedStatementCloseCount);
        assertEquals(1, recording.connectionCloseCount);
    }

    @Test
    void configuration_appliesFetchSizeQueryTimeoutAndReadOnly() throws Exception {
        Recording recording = new Recording("jdbc:h2:mem:mog-cursor-config;DB_CLOSE_DELAY=-1");
        MogJdbcDataSource dataSource = proxyDataSource(recording);
        dataSource.setFetchSize(250);
        dataSource.setQueryTimeoutSeconds(30);
        dataSource.setReadOnly(true);

        try (MogFetchCursor cursor = dataSource.openCursor(new MogQueryFilter("select 1 as id"), context())) {
            assertTrue(cursor.next());
        }

        assertEquals(250, recording.fetchSize);
        assertEquals(30, recording.queryTimeoutSeconds);
        assertEquals(Boolean.TRUE, recording.readOnly);
    }

    @Test
    void namedParameters_bindInOrderAndRepeatedParametersBindRepeatedly() throws Exception {
        Recording recording = new Recording("jdbc:h2:mem:mog-cursor-params;DB_CLOSE_DELAY=-1");
        MogJdbcDataSource dataSource = proxyDataSource(recording);
        MogQueryFilter filter = new MogQueryFilter(
                "select :cycleId + :afterId + :cycleId as total_value");
        filter.setParameters(Map.of(
                "cycleId", new MogQueryParameter("${cycleId}"),
                "afterId", new MogQueryParameter("${afterId}")));

        MogContext context = MogContext.builder()
                .variable("cycleId", Integer.valueOf(10))
                .variable("afterId", Integer.valueOf(5))
                .build();

        try (MogFetchCursor cursor = dataSource.openCursor(filter, context)) {
            assertTrue(cursor.next());
            assertEquals(25, ((Number) cursor.current().get("total_value")).intValue());
        }

        assertEquals("select ? + ? + ? as total_value", recording.preparedSql);
        assertEquals(List.of(10, 5, 10), recording.boundValues);
    }

    @Test
    void maliciousStringParameter_remainsDataAndDoesNotChangeSqlStructure() throws Exception {
        String url = "jdbc:h2:mem:mog-cursor-malicious;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            connection.createStatement().execute("create table person(name varchar(100))");
            connection.createStatement().execute("insert into person(name) values ('alpha')");
        }

        MogJdbcDataSource dataSource = h2DataSource(url);
        MogQueryFilter filter = new MogQueryFilter("select count(*) as match_count from person where name = :name");
        filter.setParameters(Map.of("name", new MogQueryParameter("${name}")));

        try (MogFetchCursor cursor = dataSource.openCursor(filter,
                MogContext.builder().variable("name", "x' or 1=1 --").build())) {
            assertTrue(cursor.next());
            assertEquals(0, ((Number) cursor.current().get("match_count")).intValue());
        }
    }

    @Test
    void fetch_drainsOpenCursorForBackwardCompatibility() throws Exception {
        MogJdbcDataSource dataSource = h2DataSource("jdbc:h2:mem:mog-cursor-fetch;DB_CLOSE_DELAY=-1");

        List<MogFetchable> rows = dataSource.fetch(new MogQueryFilter("select 1 as id, 'alpha' as name"), context());

        assertEquals(1, rows.size());
        assertEquals(1, ((Number) rows.get(0).get("id")).intValue());
        assertEquals("alpha", rows.get(0).get("name"));
    }

    private MogJdbcDataSource h2DataSource(String url) {
        MogJdbcDataSource dataSource = new MogJdbcDataSource();
        dataSource.setType("jdbc");
        dataSource.setUrl(url);
        dataSource.setUser("sa");
        dataSource.setPassword(null);
        return dataSource;
    }

    private MogJdbcDataSource proxyDataSource(Recording recording) {
        ProxyDriver.recordings.put(recording.proxyUrl(), recording);
        return h2DataSource(recording.proxyUrl());
    }

    private MogContext context() {
        return MogContext.builder().build();
    }

    private static final class Recording {
        private final String delegateUrl;
        private String preparedSql;
        private int resultSetType;
        private int resultSetConcurrency;
        private Integer fetchSize;
        private Integer queryTimeoutSeconds;
        private Boolean readOnly;
        private int resultSetCloseCount;
        private int preparedStatementCloseCount;
        private int connectionCloseCount;
        private boolean throwOnExecuteQuery;
        private boolean throwOnNext;
        private final java.util.ArrayList<Object> boundValues = new java.util.ArrayList<>();

        private Recording(String delegateUrl) {
            this.delegateUrl = delegateUrl;
        }

        private String proxyUrl() {
            return ProxyDriver.PREFIX + delegateUrl.substring("jdbc:".length());
        }
    }

    private static final class ProxyDriver implements Driver {
        private static final String PREFIX = "jdbc:mog-proxy:";
        private static final Map<String, Recording> recordings = new ConcurrentHashMap<>();
        private static boolean registered = false;

        private static synchronized void register() throws SQLException {
            if (!registered) {
                DriverManager.registerDriver(new ProxyDriver());
                registered = true;
            }
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            if (!acceptsURL(url)) {
                return null;
            }
            Recording recording = recordings.get(url);
            if (recording == null) {
                throw new SQLException("No recording for " + url);
            }
            Connection delegate = DriverManager.getConnection(recording.delegateUrl, info);
            return proxy(Connection.class, (proxy, method, args) -> {
                String name = method.getName();
                if ("prepareStatement".equals(name) && args != null && args.length >= 3
                        && args[0] instanceof String sql) {
                    recording.preparedSql = sql;
                    recording.resultSetType = (Integer) args[1];
                    recording.resultSetConcurrency = (Integer) args[2];
                    PreparedStatement prepared = (PreparedStatement) method.invoke(delegate, args);
                    return preparedStatementProxy(prepared, recording);
                }
                if ("setReadOnly".equals(name)) {
                    recording.readOnly = (Boolean) args[0];
                }
                if ("close".equals(name)) {
                    recording.connectionCloseCount++;
                }
                return method.invoke(delegate, args);
            });
        }

        private PreparedStatement preparedStatementProxy(PreparedStatement delegate, Recording recording) {
            return proxy(PreparedStatement.class, (proxy, method, args) -> {
                String name = method.getName();
                if ("setFetchSize".equals(name)) {
                    recording.fetchSize = (Integer) args[0];
                } else if ("setQueryTimeout".equals(name)) {
                    recording.queryTimeoutSeconds = (Integer) args[0];
                } else if (isBind(name, args)) {
                    int parameterIndex = (Integer) args[0];
                    while (recording.boundValues.size() < parameterIndex) {
                        recording.boundValues.add(null);
                    }
                    recording.boundValues.set(parameterIndex - 1, args.length > 1 ? args[1] : null);
                } else if ("executeQuery".equals(name)) {
                    if (recording.throwOnExecuteQuery) {
                        throw new SQLException("execute failed");
                    }
                    ResultSet resultSet = (ResultSet) method.invoke(delegate, args);
                    return resultSetProxy(resultSet, recording);
                } else if ("close".equals(name)) {
                    recording.preparedStatementCloseCount++;
                }
                return method.invoke(delegate, args);
            });
        }

        private ResultSet resultSetProxy(ResultSet delegate, Recording recording) {
            return proxy(ResultSet.class, (proxy, method, args) -> {
                String name = method.getName();
                if ("next".equals(name) && recording.throwOnNext) {
                    throw new SQLException("next failed");
                }
                if ("close".equals(name)) {
                    recording.resultSetCloseCount++;
                }
                return method.invoke(delegate, args);
            });
        }

        private boolean isBind(String name, Object[] args) {
            return args != null && args.length >= 2 && args[0] instanceof Integer
                    && (name.startsWith("set") || "setObject".equals(name));
        }

        @SuppressWarnings("unchecked")
        private <T> T proxy(Class<T> type, InvocationHandler handler) {
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith(PREFIX);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }
    }
}
