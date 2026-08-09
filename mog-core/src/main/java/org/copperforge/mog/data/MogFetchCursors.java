package org.copperforge.mog.data;

import java.util.ArrayList;
import java.util.List;

import org.copperforge.mog.MogException;

public final class MogFetchCursors {

    private MogFetchCursors() {
    }

    public static MogFetchCursor fromList(List<? extends MogFetchable> rows) {
        return new ListBackedMogFetchCursor(rows);
    }

    private static final class ListBackedMogFetchCursor implements MogFetchCursor {
        private final List<? extends MogFetchable> rows;
        private final List<String> columns;
        private int index = -1;
        private long rowNumber = 0;
        private boolean exhausted = false;
        private boolean closed = false;

        private ListBackedMogFetchCursor(List<? extends MogFetchable> rows) {
            this.rows = rows != null ? List.copyOf(rows) : List.of();
            this.columns = resolveColumns(this.rows);
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
                return false;
            }
            int nextIndex = index + 1;
            if (nextIndex >= rows.size()) {
                index = rows.size();
                exhausted = true;
                return false;
            }
            index = nextIndex;
            rowNumber++;
            return true;
        }

        @Override
        public MogFetchable current() throws MogException {
            ensureOpen();
            if (index < 0 || exhausted || index >= rows.size()) {
                throw new MogException("Cursor is not positioned on a row");
            }
            return rows.get(index);
        }

        @Override
        public long rowNumber() {
            return rowNumber;
        }

        @Override
        public void close() throws MogException {
            closed = true;
        }

        private void ensureOpen() throws MogException {
            if (closed) {
                throw new MogException("Cursor is closed");
            }
        }

        private static List<String> resolveColumns(List<? extends MogFetchable> rows) {
            if (rows == null || rows.isEmpty() || rows.get(0) == null) {
                return List.of();
            }
            return List.copyOf(new ArrayList<>(rows.get(0).keys()));
        }
    }
}
