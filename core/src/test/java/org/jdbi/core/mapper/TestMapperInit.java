/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.core.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.junit5.H2DatabaseExtension;
import org.jdbi.core.statement.StatementContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class TestMapperInit {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @BeforeEach
    public void setUp() {
        Handle handle = h2Extension.getSharedHandle();

        handle.execute("create table column_mappers (string_value varchar, long_value integer)");
        handle.execute("insert into column_mappers (string_value, long_value) values (?, ?)", "foo", 1L);
        handle.execute("insert into column_mappers (string_value, long_value) values (?, ?)", "bar", 2L);
        handle.execute("insert into column_mappers (string_value, long_value) values (?, ?)", "baz", 3L);
    }

    @Test
    public void testColumnMapper() {
        final StringValueMapper mapper = new StringValueMapper();
        // not yet initialized
        assertThat(mapper.getInitializedCount()).isZero();
        assertThat(mapper.getMappedCount()).isZero();

        Jdbi jdbi = h2Extension.getJdbi();
        jdbi.registerColumnMapper(StringValue.class, mapper);

        // still not initialized, only at first retrieval
        assertThat(mapper.getInitializedCount()).isZero();
        assertThat(mapper.getMappedCount()).isZero();

        List<StringValue> values = jdbi.withHandle(h -> {
            List<StringValue> value = h.createQuery("SELECT string_value FROM column_mappers")
                .mapTo(StringValue.class)
                .list();

            // has been called once
            assertThat(mapper.getInitializedCount()).isOne();

            // has been called for every row (mapper gets reused)
            assertThat(value).hasSize(mapper.getMappedCount());

            value = h.createQuery("SELECT string_value FROM column_mappers")
                .mapTo(StringValue.class)
                .list();

            // has been called once
            assertThat(mapper.getInitializedCount()).isEqualTo(2);

            // has been called for every row (mapper gets reused)
            assertThat(value.size() * 2).isEqualTo(mapper.getMappedCount());
            return value;
        });

        assertThat(values)
                .isNotNull()
                .hasSize(3)
                .contains(new StringValue("foo"), new StringValue("bar"), new StringValue("baz"));

        // redo with another handle
        jdbi.useHandle(h -> {
            List<StringValue> value = h.createQuery("SELECT string_value FROM column_mappers")
                .mapTo(StringValue.class)
                .list();

            // called again for the statement
            assertThat(mapper.getInitializedCount()).isEqualTo(3);

            // has been called for every row again (mapper gets reused)
            assertThat(value.size() * 3).isEqualTo(mapper.getMappedCount());
        });
    }

    @Test
    public void testRowMapper() {
        final GenericType<Map.Entry<StringValue, Integer>> resultType = new GenericType<Entry<StringValue, Integer>>() {};

        final StringValueMapper mapper = new StringValueMapper();
        // not yet initialized
        assertThat(mapper.getInitializedCount()).isZero();
        assertThat(mapper.getMappedCount()).isZero();

        Jdbi jdbi = h2Extension.getJdbi();
        jdbi.registerColumnMapper(StringValue.class, mapper);
        jdbi.registerRowMapper(resultType, new ResultMapper());

        // still not initialized, only at first retrieval
        assertThat(mapper.getInitializedCount()).isZero();
        assertThat(mapper.getMappedCount()).isZero();

        List<Map.Entry<StringValue, Integer>> values = jdbi.withHandle(h -> {
            List<Map.Entry<StringValue, Integer>> value = h.createQuery("SELECT * FROM column_mappers")
                .mapTo(resultType)
                .list();

            // has been called once
            assertThat(mapper.getInitializedCount()).isOne();

            // has been called for every row (mapper gets reused)
            assertThat(value).hasSize(mapper.getMappedCount());

            value = h.createQuery("SELECT * FROM column_mappers")
                .mapTo(resultType)
                .list();

            // has been called once
            assertThat(mapper.getInitializedCount()).isEqualTo(2);

            // has been called for every row (mapper gets reused)
            assertThat(value.size() * 2).isEqualTo(mapper.getMappedCount());
            return value;
        });

        assertThat(values)
                .isNotNull()
                .hasSize(3);

        // redo with another handle
        jdbi.useHandle(h -> {
            List<Map.Entry<StringValue, Integer>> value = h.createQuery("SELECT * FROM column_mappers")
                .mapTo(resultType)
                .list();
            // called again for the statement
            assertThat(mapper.getInitializedCount()).isEqualTo(3);

            // has been called for every row again (mapper gets reused)
            assertThat(value).hasSize(mapper.getMappedCount() / 3);
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
            return new SimpleImmutableEntry<>(
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
