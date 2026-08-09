package org.copperforge.mog.data.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MogNamedSqlParserTest {

    private final MogNamedSqlParser parser = new MogNamedSqlParser();

    @Test
    void parse_zeroParameters() throws Exception {
        MogPreparedSql prepared = parser.parse("select * from claim where status = 'A'");

        assertEquals("select * from claim where status = 'A'", prepared.sql());
        assertEquals(java.util.List.of(), prepared.parameterNames());
    }

    @Test
    void parse_oneParameter() throws Exception {
        MogPreparedSql prepared = parser.parse("select * from claim where cycle_id = :cycleId");

        assertEquals("select * from claim where cycle_id = ?", prepared.sql());
        assertEquals(java.util.List.of("cycleId"), prepared.parameterNames());
    }

    @Test
    void parse_severalParameters() throws Exception {
        MogPreparedSql prepared = parser.parse(
                "select * from claim where cycle_id = :cycleId and id > :afterId");

        assertEquals("select * from claim where cycle_id = ? and id > ?", prepared.sql());
        assertEquals(java.util.List.of("cycleId", "afterId"), prepared.parameterNames());
    }

    @Test
    void parse_repeatedParameter_preservesOccurrences() throws Exception {
        MogPreparedSql prepared = parser.parse(
                "select * from claim where cycle_id = :cycleId or parent_cycle_id = :cycleId");

        assertEquals("select * from claim where cycle_id = ? or parent_cycle_id = ?", prepared.sql());
        assertEquals(java.util.List.of("cycleId", "cycleId"), prepared.parameterNames());
    }

    @Test
    void parse_ignoresColonInsideSingleQuotedString() throws Exception {
        MogPreparedSql prepared = parser.parse("select ':notAParam', name from claim where id = :id");

        assertEquals("select ':notAParam', name from claim where id = ?", prepared.sql());
        assertEquals(java.util.List.of("id"), prepared.parameterNames());
    }

    @Test
    void parse_ignoresColonInsideDoubleQuotedIdentifier() throws Exception {
        MogPreparedSql prepared = parser.parse("select \"schema:table\" from claim where id = :id");

        assertEquals("select \"schema:table\" from claim where id = ?", prepared.sql());
        assertEquals(java.util.List.of("id"), prepared.parameterNames());
    }

    @Test
    void parse_ignoresColonInsideLineComment() throws Exception {
        MogPreparedSql prepared = parser.parse("select * from claim -- :ignored\nwhere id = :id");

        assertEquals("select * from claim -- :ignored\nwhere id = ?", prepared.sql());
        assertEquals(java.util.List.of("id"), prepared.parameterNames());
    }

    @Test
    void parse_ignoresColonInsideBlockComment() throws Exception {
        MogPreparedSql prepared = parser.parse("select * from claim /* :ignored */ where id = :id");

        assertEquals("select * from claim /* :ignored */ where id = ?", prepared.sql());
        assertEquals(java.util.List.of("id"), prepared.parameterNames());
    }

    @Test
    void parse_preservesPostgreSqlStyleCast() throws Exception {
        MogPreparedSql prepared = parser.parse("select :cycleId::bigint as cycle_id");

        assertEquals("select ?::bigint as cycle_id", prepared.sql());
        assertEquals(java.util.List.of("cycleId"), prepared.parameterNames());
    }
}
