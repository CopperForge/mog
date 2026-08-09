package org.copperforge.mog.data.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.JDBCType;
import java.util.List;
import java.util.Map;

import org.copperforge.mog.MogException;
import org.copperforge.mog.data.filter.MogQueryParameter;
import org.copperforge.mog.runtime.MogContext;
import org.junit.jupiter.api.Test;

class MogSqlParameterResolverTest {

    private final MogSqlParameterResolver resolver = new MogSqlParameterResolver();

    @Test
    void resolve_exactPlaceholderPreservesContextJavaTypes() throws Exception {
        MogContext context = MogContext.builder()
                .variable("intValue", Integer.valueOf(7))
                .variable("longValue", Long.valueOf(8L))
                .variable("booleanValue", Boolean.TRUE)
                .variable("decimalValue", new BigDecimal("9.50"))
                .build();

        List<MogResolvedSqlParameter> resolved = resolver.resolve(
                prepared("intValue", "longValue", "booleanValue", "decimalValue"),
                Map.of(
                        "intValue", new MogQueryParameter("${intValue}"),
                        "longValue", new MogQueryParameter("${longValue}"),
                        "booleanValue", new MogQueryParameter("${booleanValue}"),
                        "decimalValue", new MogQueryParameter("${decimalValue}")),
                context);

        assertInstanceOf(Integer.class, resolved.get(0).value());
        assertInstanceOf(Long.class, resolved.get(1).value());
        assertInstanceOf(Boolean.class, resolved.get(2).value());
        assertInstanceOf(BigDecimal.class, resolved.get(3).value());
    }

    @Test
    void resolve_embeddedPlaceholderBecomesString() throws Exception {
        MogContext context = MogContext.builder().variable("cycleId", Integer.valueOf(42)).build();

        List<MogResolvedSqlParameter> resolved = resolver.resolve(
                prepared("name"),
                Map.of("name", new MogQueryParameter("cycle-${cycleId}")),
                context);

        assertEquals("cycle-42", resolved.get(0).value());
        assertInstanceOf(String.class, resolved.get(0).value());
    }

    @Test
    void resolve_missingContextVariableFailsClearly() {
        MogException ex = assertThrows(MogException.class,
                () -> resolver.resolve(
                        prepared("cycleId"),
                        Map.of("cycleId", new MogQueryParameter("${__MOG_MISSING_TEST_VAR__}")),
                        MogContext.builder().build()));

        assertTrue(ex.getMessage().contains("unresolved variable"));
        assertTrue(ex.getMessage().contains("cycleId"));
    }

    @Test
    void resolve_missingSqlParameterDefinitionFailsClearly() {
        MogException ex = assertThrows(MogException.class,
                () -> resolver.resolve(prepared("cycleId"), Map.of(), MogContext.builder().build()));

        assertTrue(ex.getMessage().contains("Missing SQL parameter definition"));
        assertTrue(ex.getMessage().contains("cycleId"));
    }

    @Test
    void resolve_unusedParameterDefinitionsAreAccepted() throws Exception {
        List<MogResolvedSqlParameter> resolved = resolver.resolve(
                prepared("cycleId"),
                Map.of(
                        "cycleId", new MogQueryParameter("7"),
                        "unused", new MogQueryParameter("ignored")),
                MogContext.builder().build());

        assertEquals(1, resolved.size());
        assertEquals("7", resolved.get(0).value());
    }

    @Test
    void resolve_unsupportedComplexTypeFailsClearly() {
        MogContext context = MogContext.builder().variable("ids", List.of(1, 2, 3)).build();

        MogException ex = assertThrows(MogException.class,
                () -> resolver.resolve(
                        prepared("ids"),
                        Map.of("ids", new MogQueryParameter("${ids}")),
                        context));

        assertTrue(ex.getMessage().contains("Unsupported value type"));
        assertTrue(ex.getMessage().contains("ids"));
    }

    @Test
    void resolve_explicitJdbcTypeUsesJdbcTypeNames() throws Exception {
        List<MogResolvedSqlParameter> resolved = resolver.resolve(
                prepared("cycleId"),
                Map.of("cycleId", new MogQueryParameter("${cycleId}", "BIGINT")),
                MogContext.builder().variable("cycleId", Long.valueOf(10L)).build());

        assertEquals(JDBCType.BIGINT, resolved.get(0).jdbcType());
    }

    @Test
    void resolve_repeatedParameterPreservesBindingOrder() throws Exception {
        List<MogResolvedSqlParameter> resolved = resolver.resolve(
                prepared("cycleId", "afterId", "cycleId"),
                Map.of(
                        "cycleId", new MogQueryParameter("${cycleId}"),
                        "afterId", new MogQueryParameter("${afterId}")),
                MogContext.builder()
                        .variable("cycleId", Long.valueOf(10L))
                        .variable("afterId", Long.valueOf(20L))
                        .build());

        assertEquals("cycleId", resolved.get(0).name());
        assertEquals("afterId", resolved.get(1).name());
        assertEquals("cycleId", resolved.get(2).name());
        assertEquals(10L, resolved.get(0).value());
        assertEquals(20L, resolved.get(1).value());
        assertEquals(10L, resolved.get(2).value());
    }

    private MogPreparedSql prepared(String... names) {
        return new MogPreparedSql("select ?", List.of(names));
    }
}
