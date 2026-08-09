package org.copperforge.mog.data.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class MogQueryFilterParameterSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void existingQueryFilterJson_deserializesWithoutParameters() throws Exception {
        MogQueryFilter filter = objectMapper.readValue(
                "{\"type\":\"query\",\"query\":\"select 1 as id\"}",
                MogQueryFilter.class);

        assertEquals("select 1 as id", filter.getQuery());
        assertTrue(filter.getParameters().isEmpty());
    }

    @Test
    void parameterizedQueryFilterJson_roundTrips() throws Exception {
        String json = """
                {
                  "type": "query",
                  "query": "select * from claim where cycle_id = :cycleId and id > :afterId",
                  "parameters": {
                    "cycleId": "${cycleId}",
                    "afterId": {
                      "value": "${afterId}",
                      "jdbcType": "BIGINT"
                    }
                  }
                }
                """;

        MogQueryFilter filter = objectMapper.readValue(json, MogQueryFilter.class);

        assertEquals("${cycleId}", filter.getParameters().get("cycleId").getValue());
        assertEquals("${afterId}", filter.getParameters().get("afterId").getValue());
        assertEquals("BIGINT", filter.getParameters().get("afterId").getJdbcType());

        String serialized = objectMapper.writeValueAsString(filter);
        MogQueryFilter roundTripped = objectMapper.readValue(serialized, MogQueryFilter.class);

        assertEquals(filter.getQuery(), roundTripped.getQuery());
        assertEquals(filter.getParameters(), roundTripped.getParameters());
    }
}
