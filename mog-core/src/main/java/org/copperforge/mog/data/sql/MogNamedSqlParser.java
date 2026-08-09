package org.copperforge.mog.data.sql;

import java.util.ArrayList;
import java.util.List;

import org.copperforge.mog.MogException;

public class MogNamedSqlParser {

    public MogPreparedSql parse(String sql) throws MogException {
        if (sql == null) {
            throw new MogException("SQL query must not be null");
        }

        StringBuilder prepared = new StringBuilder(sql.length());
        List<String> parameters = new ArrayList<>();
        State state = State.NORMAL;

        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';
            char previous = i > 0 ? sql.charAt(i - 1) : '\0';

            switch (state) {
                case NORMAL -> {
                    if (current == '\'') {
                        prepared.append(current);
                        state = State.SINGLE_QUOTED;
                    } else if (current == '"') {
                        prepared.append(current);
                        state = State.DOUBLE_QUOTED;
                    } else if (current == '-' && next == '-') {
                        prepared.append(current).append(next);
                        i++;
                        state = State.LINE_COMMENT;
                    } else if (current == '/' && next == '*') {
                        prepared.append(current).append(next);
                        i++;
                        state = State.BLOCK_COMMENT;
                    } else if (current == ':' && previous != ':' && isParameterStart(next)) {
                        int start = i + 1;
                        int end = start + 1;
                        while (end < sql.length() && isParameterPart(sql.charAt(end))) {
                            end++;
                        }
                        String name = sql.substring(start, end);
                        parameters.add(name);
                        prepared.append('?');
                        i = end - 1;
                    } else {
                        prepared.append(current);
                    }
                }
                case SINGLE_QUOTED -> {
                    prepared.append(current);
                    if (current == '\'' && next == '\'') {
                        prepared.append(next);
                        i++;
                    } else if (current == '\'') {
                        state = State.NORMAL;
                    }
                }
                case DOUBLE_QUOTED -> {
                    prepared.append(current);
                    if (current == '"' && next == '"') {
                        prepared.append(next);
                        i++;
                    } else if (current == '"') {
                        state = State.NORMAL;
                    }
                }
                case LINE_COMMENT -> {
                    prepared.append(current);
                    if (current == '\n' || current == '\r') {
                        state = State.NORMAL;
                    }
                }
                case BLOCK_COMMENT -> {
                    prepared.append(current);
                    if (current == '*' && next == '/') {
                        prepared.append(next);
                        i++;
                        state = State.NORMAL;
                    }
                }
            }
        }

        return new MogPreparedSql(prepared.toString(), parameters);
    }

    private boolean isParameterStart(char value) {
        return value == '_' || Character.isLetter(value);
    }

    private boolean isParameterPart(char value) {
        return value == '_' || Character.isLetterOrDigit(value);
    }

    private enum State {
        NORMAL,
        SINGLE_QUOTED,
        DOUBLE_QUOTED,
        LINE_COMMENT,
        BLOCK_COMMENT
    }
}
