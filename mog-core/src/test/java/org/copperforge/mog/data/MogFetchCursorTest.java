package org.copperforge.mog.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.copperforge.mog.MogException;
import org.copperforge.mog.data.filter.MogDataFilter;
import org.copperforge.mog.runtime.MogContext;
import org.junit.jupiter.api.Test;

class MogFetchCursorTest {

    @Test
    void openCursor_iteratesZeroRows() throws Exception {
        MogFetchCursor cursor = datasource(List.of()).openCursor(new MogDataFilter(), context());

        assertEquals(List.of(), cursor.columns());
        assertEquals(0, cursor.rowNumber());
        assertFalse(cursor.next());
        assertEquals(0, cursor.rowNumber());
        assertThrows(MogException.class, cursor::current);
    }

    @Test
    void openCursor_iteratesOneRow() throws Exception {
        MogFetchable row = row("id", 1);
        MogFetchCursor cursor = datasource(List.of(row)).openCursor(new MogDataFilter(), context());

        assertEquals(List.of("id"), cursor.columns());
        assertEquals(0, cursor.rowNumber());
        assertTrue(cursor.next());
        assertEquals(1, cursor.rowNumber());
        assertSame(row, cursor.current());
        assertFalse(cursor.next());
        assertEquals(1, cursor.rowNumber());
    }

    @Test
    void openCursor_iteratesMultipleRows() throws Exception {
        MogFetchable first = row("id", 1);
        MogFetchable second = row("id", 2);
        MogFetchable third = row("id", 3);
        MogFetchCursor cursor = datasource(List.of(first, second, third)).openCursor(new MogDataFilter(), context());

        assertTrue(cursor.next());
        assertSame(first, cursor.current());
        assertEquals(1, cursor.rowNumber());

        assertTrue(cursor.next());
        assertSame(second, cursor.current());
        assertEquals(2, cursor.rowNumber());

        assertTrue(cursor.next());
        assertSame(third, cursor.current());
        assertEquals(3, cursor.rowNumber());

        assertFalse(cursor.next());
        assertEquals(3, cursor.rowNumber());
    }

    @Test
    void columns_usesFirstRowKeys_withoutAdvancing() throws Exception {
        MogFetchable row = new MogFetchable(Map.of("id", 1, "name", "alpha"));
        MogFetchCursor cursor = datasource(List.of(row)).openCursor(new MogDataFilter(), context());

        List<String> columns = cursor.columns();

        assertEquals(0, cursor.rowNumber());
        assertEquals(2, columns.size());
        assertTrue(columns.contains("id"));
        assertTrue(columns.contains("name"));
        assertTrue(cursor.next());
        assertSame(row, cursor.current());
    }

    @Test
    void current_throwsBeforeFirstNextAndAfterExhaustion() throws Exception {
        MogFetchCursor cursor = datasource(List.of(row("id", 1))).openCursor(new MogDataFilter(), context());

        assertThrows(MogException.class, cursor::current);
        assertTrue(cursor.next());
        assertEquals(1, cursor.current().get("id"));
        assertFalse(cursor.next());
        assertThrows(MogException.class, cursor::current);
    }

    @Test
    void close_isIdempotent() throws Exception {
        MogFetchCursor cursor = datasource(List.of(row("id", 1))).openCursor(new MogDataFilter(), context());

        cursor.close();
        cursor.close();

        assertEquals(0, cursor.rowNumber());
    }

    @Test
    void operationsAfterClose_throwMogException() throws Exception {
        MogFetchCursor cursor = datasource(List.of(row("id", 1))).openCursor(new MogDataFilter(), context());

        assertTrue(cursor.next());
        cursor.close();

        assertEquals(1, cursor.rowNumber());
        assertThrows(MogException.class, cursor::columns);
        assertThrows(MogException.class, cursor::next);
        assertThrows(MogException.class, cursor::current);
        cursor.close();
    }

    @Test
    void openCursor_propagatesFetchMogException() {
        MogException expected = new MogException("fetch failed");
        MogDataSource dataSource = new StubDataSource(null, expected);

        MogException actual = assertThrows(MogException.class,
                () -> dataSource.openCursor(new MogDataFilter(), context()));

        assertSame(expected, actual);
    }

    private static MogContext context() {
        return MogContext.builder().build();
    }

    private static MogFetchable row(String key, Object value) {
        return new MogFetchable(Map.of(key, value));
    }

    private static MogDataSource datasource(List<? extends MogFetchable> rows) {
        return new StubDataSource(rows, null);
    }

    private static final class StubDataSource extends MogDataSource {
        private final List<? extends MogFetchable> rows;
        private final MogException exception;

        private StubDataSource(List<? extends MogFetchable> rows, MogException exception) {
            this.rows = rows;
            this.exception = exception;
        }

        @Override
        public List<? extends MogFetchable> fetch(MogDataFilter filter, MogContext context) throws MogException {
            if (exception != null) {
                throw exception;
            }
            return rows;
        }
    }
}
