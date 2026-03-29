package org.copperforge.mog.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.copperforge.mog.data.filter.MogQueryFilter;
import org.copperforge.mog.runtime.MogContext;
import org.junit.jupiter.api.Test;

class MogJdbcDataSourceTest {

    @Test
    void fetch_executesSimpleQuery_withoutServiceManagerWiring() throws Exception {
        MogJdbcDataSource dataSource = new MogJdbcDataSource();
        dataSource.setType("jdbc");
        dataSource.setUrl("jdbc:h2:mem:mog-jdbc-test;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword(null);

        MogQueryFilter filter = new MogQueryFilter("select 1 as id, 'alpha' as name");

        List<? extends MogFetchable> rows = dataSource.fetch(filter, MogContext.builder().build());

        assertEquals(1, rows.size());
        assertEquals(1, ((Number) rows.get(0).get("id")).intValue());
        assertEquals("alpha", rows.get(0).get("name"));
    }
}
