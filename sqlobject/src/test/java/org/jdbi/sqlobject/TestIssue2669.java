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
package org.jdbi.sqlobject;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.core.Jdbi;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.mapper.ColumnMapper;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.mapper.RowMapperFactory;
import org.jdbi.core.mapper.reflect.JdbiConstructor;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.util.Objects.requireNonNull;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that null does not auto-propagate from a database column value to a wrapped optional. See <a href="https://github.com/jdbi/jdbi/issues/2669">Issue
 * #2669</a>.
 */
public class TestIssue2669 {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg)
            .withInitializer((ds, handle) -> handle.execute("CREATE TABLE foo (key VARCHAR NOT NULL PRIMARY KEY, value VARCHAR)"))
            .withPlugins(new SqlObjectPlugin());

    private Jdbi jdbi;

    @BeforeEach
    public void setUp() {
        this.jdbi = pgExtension.getJdbi();

        jdbi.registerColumnMapper(Value.class, ColumnMapper.getDefaultColumnMapper())
                .registerRowMapper((RowMapperFactory) new ValueMapper());
    }

    @Test
    public void testIssue2669() {
        jdbi.useExtension(ValueDao.class, dao -> dao.insert("1", null));
        jdbi.useExtension(ValueDao.class, dao -> dao.insert("2", "foo"));

        assertThat((Optional<Value>) jdbi.withExtension(ValueDao.class, dao -> dao.get("1"))).isEmpty();
        assertThat((Optional<Value>) jdbi.withExtension(ValueDao.class, dao -> dao.get("2"))).isPresent().contains(new Value("foo"));
    }

    public record Value(String value) {
        @JdbiConstructor
        public Value {
            requireNonNull(value, "value is null");
        }
    }

    public interface ValueDao {
        @SqlQuery("SELECT value FROM foo where key = :key")
        Optional<Value> get(@Bind("key") String key);

        @SqlUpdate("INSERT INTO foo(key, value) VALUES (:key, :value)")
        void insert(@Bind("key") String key, @Bind("value") String value);
    }

    public static class ValueMapper
            implements RowMapperFactory, RowMapper<Value> {
        @Override
        public Optional<RowMapper<?>> build(Type type, ConfigRegistry config) {
            if (type.equals(Value.class)) {
                return Optional.of(this);
            }
            return Optional.empty();
        }

        @Override
        public Value map(ResultSet rs, StatementContext ctx)
                throws SQLException {
            String value = rs.getString("value");
            return (rs.wasNull()) ? null : new Value(value);
        }
    }
}
