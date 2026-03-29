package org.copperforge.mog.data;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.copperforge.mog.MogException;
import org.copperforge.mog.data.dotout.MogDotOutDataSource;
import org.copperforge.mog.data.filter.MogJsonFilter;
import org.copperforge.mog.data.filter.MogQueryFilter;
import org.copperforge.mog.runtime.MogContext;
import org.junit.jupiter.api.Test;

class MogDataSourceFilterValidationTest {

    @Test
    void jsonDatasource_rejectsQueryFilterWithClearMessage() throws Exception {
        MogJsonDataSource dataSource = new MogJsonDataSource();
        dataSource.setName("jsonSource");
        dataSource.setType("json");
        dataSource.setFile("ignored.json");

        MogQueryFilter filter = new MogQueryFilter();
        filter.setType("query");
        filter.setQuery("id = 1");

        MogException ex = assertThrows(MogException.class,
                () -> dataSource.fetch(filter, MogContext.builder().build()));

        assertTrue(ex.getMessage().contains("requires filter type 'json'"));
        assertTrue(ex.getMessage().contains("received 'query'"));
    }

    @Test
    void jdbcDatasource_rejectsJsonFilterWithClearMessage() {
        MogJdbcDataSource dataSource = new MogJdbcDataSource();
        dataSource.setName("jdbcSource");
        dataSource.setType("jdbc");
        dataSource.setUrl("jdbc:h2:mem:test");

        MogJsonFilter filter = new MogJsonFilter();
        filter.setType("json");
        filter.setJsonPath("$.data[*]");

        MogException ex = assertThrows(MogException.class,
                () -> dataSource.fetch(filter, MogContext.builder().build()));

        assertTrue(ex.getMessage().contains("requires filter type 'query'"));
        assertTrue(ex.getMessage().contains("received 'json'"));
    }

    @Test
    void dotOutDatasource_rejectsJsonFilterWithClearMessage() {
        MogDotOutDataSource dataSource = new MogDotOutDataSource();
        dataSource.setName("dotoutSource");
        dataSource.setType("dotout");
        dataSource.setFile("ignored.dat");
        dataSource.setFormat("ignored");

        MogJsonFilter filter = new MogJsonFilter();
        filter.setType("json");
        filter.setJsonPath("$.data[*]");

        MogException ex = assertThrows(MogException.class,
                () -> dataSource.fetch(filter, MogContext.builder().build()));

        assertTrue(ex.getMessage().contains("requires filter type 'query'"));
        assertTrue(ex.getMessage().contains("received 'json'"));
    }
}
