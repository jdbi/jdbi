package org.jdbi.v3.core.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.StatementContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestMapperInit {

    @Rule
    public H2DatabaseRule rule = new H2DatabaseRule();

    @Before
    public void setUp() {
        Handle handle = rule.getSharedHandle();

        handle.execute("create table column_mappers (string_value varchar, long_value integer)");
        handle.execute("insert into column_mappers (string_value, long_value) values (?, ?)", "foo", 1L);
        handle.execute("insert into column_mappers (string_value, long_value) values (?, ?)", "bar", 2L);
        handle.execute("insert into column_mappers (string_value, long_value) values (?, ?)", "baz", 3L);
    }

    @Test
    public void testColumnMapper() {
        final StringValueMapper mapper = new StringValueMapper();
        // not yet initialized
        assertEquals(0, mapper.getInitializedCount());
        assertEquals(0, mapper.getMappedCount());

        Jdbi jdbi = rule.getJdbi();
        jdbi.registerColumnMapper(StringValue.class, mapper);

        // still not initialized, only at first retrieval
        assertEquals(0, mapper.getInitializedCount());
        assertEquals(0, mapper.getMappedCount());

        List<StringValue> values = jdbi.withHandle(h -> {
            List<StringValue> value = h.createQuery("SELECT string_value FROM column_mappers")
                .mapTo(StringValue.class)
                .list();

            // has been called once
            assertEquals(1, mapper.getInitializedCount());

            // has been called for every row (mapper gets reused)
            assertEquals(value.size(), mapper.getMappedCount());

            value = h.createQuery("SELECT string_value FROM column_mappers")
                .mapTo(StringValue.class)
                .list();

            // has been called once
            assertEquals(2, mapper.getInitializedCount());

            // has been called for every row (mapper gets reused)
            assertEquals(value.size() * 2, mapper.getMappedCount());
            return value;
        });

        assertNotNull(values);
        assertEquals(3, values.size());
        assertTrue(values.contains(new StringValue("foo")));
        assertTrue(values.contains(new StringValue("bar")));
        assertTrue(values.contains(new StringValue("baz")));

        // redo with another handle
        values = jdbi.withHandle(h -> {
            List<StringValue> value = h.createQuery("SELECT string_value FROM column_mappers")
                .mapTo(StringValue.class)
                .list();

            // called again for the statement
            assertEquals(3, mapper.getInitializedCount());

            // has been called for every row again (mapper gets reused)
            assertEquals(value.size() * 3, mapper.getMappedCount());

            return value;
        });
    }

    @Test
    public void testRowMapper() {
        final GenericType<Map.Entry<StringValue, Integer>> RESULT_TYPE = new GenericType<Entry<StringValue, Integer>>() {};

        final StringValueMapper mapper = new StringValueMapper();
        // not yet initialized
        assertEquals(0, mapper.getInitializedCount());
        assertEquals(0, mapper.getMappedCount());

        Jdbi jdbi = rule.getJdbi();
        jdbi.registerColumnMapper(StringValue.class, mapper);
        jdbi.registerRowMapper(RESULT_TYPE, new ResultMapper());

        // still not initialized, only at first retrieval
        assertEquals(0, mapper.getInitializedCount());
        assertEquals(0, mapper.getMappedCount());

        List<Map.Entry<StringValue, Integer>> values = jdbi.withHandle(h -> {
            List<Map.Entry<StringValue, Integer>> value = h.createQuery("SELECT * FROM column_mappers")
                .mapTo(RESULT_TYPE)
                .list();

            // has been called once
            assertEquals(1, mapper.getInitializedCount());

            // has been called for every row (mapper gets reused)
            assertEquals(value.size(), mapper.getMappedCount());

            value = h.createQuery("SELECT * FROM column_mappers")
                .mapTo(RESULT_TYPE)
                .list();

            // has been called once
            assertEquals(2, mapper.getInitializedCount());

            // has been called for every row (mapper gets reused)
            assertEquals(value.size() * 2, mapper.getMappedCount());
            return value;
        });

        assertNotNull(values);
        assertEquals(3, values.size());

        // redo with another handle
        values = jdbi.withHandle(h -> {
            List<Map.Entry<StringValue, Integer>> value = h.createQuery("SELECT * FROM column_mappers")
                .mapTo(RESULT_TYPE)
                .list();
            // called again for the statement
            assertEquals(3, mapper.getInitializedCount());

            // has been called for every row again (mapper gets reused)
            assertEquals(value.size() * 3, mapper.getMappedCount());

            return value;
        });
    }


    public static class StringValue {

        private final String value;

        public StringValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            StringValue that = (StringValue) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public static class StringValueMapper implements ColumnMapper<StringValue> {

        private final AtomicInteger initializedCount = new AtomicInteger(0);
        private final AtomicInteger mappedCount = new AtomicInteger(0);

        int getInitializedCount() {
            return initializedCount.get();
        }

        int getMappedCount() {
            return mappedCount.get();
        }

        @Override
        public StringValue map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
            mappedCount.incrementAndGet();
            return new StringValue(rs.getString(columnNumber));
        }

        @Override
        public void init(ConfigRegistry registry) {
            initializedCount.incrementAndGet();
        }
    }

    public static class ResultMapper implements RowMapper<Entry<StringValue, Integer>> {

        private ColumnMapper<StringValue> stringValueMapper;

        @Override
        public Entry<StringValue, Integer> map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new SimpleImmutableEntry<StringValue, Integer>(
                stringValueMapper.map(rs, 1, ctx),
                rs.getInt(2)
            );
        }

        @Override
        public void init(ConfigRegistry registry) {
            stringValueMapper = registry.get(ColumnMappers.class).findFor(StringValue.class).orElseGet(() -> fail("No mapper found!"));
        }
    }
}
