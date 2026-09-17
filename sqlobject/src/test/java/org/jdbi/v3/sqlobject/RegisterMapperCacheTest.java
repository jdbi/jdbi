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
package org.jdbi.v3.sqlobject;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterColumnMapper;
import org.jdbi.v3.sqlobject.config.RegisterColumnMapperFactory;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapperFactory;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class RegisterMapperCacheTest {
    @RegisterExtension
    public H2DatabaseExtension h2db = H2DatabaseExtension.instance()
            .withPlugin(new SqlObjectPlugin());

    static final AtomicInteger COLUMN_MAPPER_FACTORY_BUILD = new AtomicInteger(0);
    static final AtomicInteger COLUMN_MAPPER_INIT = new AtomicInteger(0);
    static final AtomicInteger ROW_MAPPER_FACTORY_BUILD = new AtomicInteger(0);
    static final AtomicInteger ROW_MAPPER_INIT = new AtomicInteger(0);

    @BeforeEach
    void reset() {
        COLUMN_MAPPER_FACTORY_BUILD.set(0);
        COLUMN_MAPPER_INIT.set(0);
        ROW_MAPPER_FACTORY_BUILD.set(0);
        ROW_MAPPER_INIT.set(0);
    }

    @Test
    @Disabled("https://github.com/jdbi/jdbi/issues/2406")
    void classColumnMapperCached() {
        for (int i = 0; i < 4; i++) {
            h2db.getJdbi().useExtension(ClassColumnMapper.class, dao -> {
                assertThat(dao.m()).isNotNull();
            });
        }
        assertThat(COLUMN_MAPPER_FACTORY_BUILD.get()).isEqualTo(0);
        assertThat(COLUMN_MAPPER_INIT.get()).isEqualTo(1);
    }

    @Test
    @Disabled("https://github.com/jdbi/jdbi/issues/2406")
    void classColumnMapperFactoryCached() {
        for (int i = 0; i < 4; i++) {
            h2db.getJdbi().useExtension(ClassColumnMapperFactory.class, dao -> {
                assertThat(dao.m()).isNotNull();
            });
        }
        assertThat(COLUMN_MAPPER_FACTORY_BUILD.get()).isEqualTo(1);
        assertThat(COLUMN_MAPPER_INIT.get()).isEqualTo(1);
    }

    @Test
    @Disabled("https://github.com/jdbi/jdbi/issues/2406")
    void methodColumnMapperCached() {
        for (int i = 0; i < 4; i++) {
            h2db.getJdbi().useExtension(MethodColumnMapper.class, dao -> {
                assertThat(dao.m()).isNotNull();
            });
        }
        assertThat(COLUMN_MAPPER_FACTORY_BUILD.get()).isEqualTo(0);
        assertThat(COLUMN_MAPPER_INIT.get()).isEqualTo(1);
    }

    @Test
    @Disabled("https://github.com/jdbi/jdbi/issues/2406")
    void methodColumnMapperFactoryCached() {
        for (int i = 0; i < 4; i++) {
            h2db.getJdbi().useExtension(MethodColumnMapperFactory.class, dao -> {
                assertThat(dao.m()).isNotNull();
            });
        }
        assertThat(COLUMN_MAPPER_FACTORY_BUILD.get()).isEqualTo(1);
        assertThat(COLUMN_MAPPER_INIT.get()).isEqualTo(1);
    }

    @Test
    @Disabled("https://github.com/jdbi/jdbi/issues/2406")
    void classRowMapperCached() {
        for (int i = 0; i < 4; i++) {
            h2db.getJdbi().useExtension(ClassRowMapper.class, dao -> {
                assertThat(dao.m()).isNotNull();
            });
        }
        assertThat(ROW_MAPPER_FACTORY_BUILD.get()).isEqualTo(0);
        assertThat(ROW_MAPPER_INIT.get()).isEqualTo(1);
    }

    @Test
    @Disabled("https://github.com/jdbi/jdbi/issues/2406")
    void classRowMapperFactoryCached() {
        for (int i = 0; i < 4; i++) {
            h2db.getJdbi().useExtension(ClassRowMapperFactory.class, dao -> {
                assertThat(dao.m()).isNotNull();
            });
        }
        assertThat(ROW_MAPPER_FACTORY_BUILD.get()).isEqualTo(1);
        assertThat(ROW_MAPPER_INIT.get()).isEqualTo(1);
    }

    @Test
    @Disabled("https://github.com/jdbi/jdbi/issues/2406")
    void methodRowMapperCached() {
        for (int i = 0; i < 4; i++) {
            h2db.getJdbi().useExtension(MethodRowMapper.class, dao -> {
                assertThat(dao.m()).isNotNull();
            });
        }
        assertThat(ROW_MAPPER_FACTORY_BUILD.get()).isEqualTo(0);
        assertThat(ROW_MAPPER_INIT.get()).isEqualTo(1);
    }

    @Test
    @Disabled("https://github.com/jdbi/jdbi/issues/2406")
    void methodRowMapperFactoryCached() {
        for (int i = 0; i < 4; i++) {
            h2db.getJdbi().useExtension(MethodRowMapperFactory.class, dao -> {
                assertThat(dao.m()).isNotNull();
            });
        }
        assertThat(ROW_MAPPER_FACTORY_BUILD.get()).isEqualTo(1);
        assertThat(ROW_MAPPER_INIT.get()).isEqualTo(1);
    }

    public static class CustomType {}

    @RegisterColumnMapper(CustomTypeColumnMapper.class)
    public interface ClassColumnMapper {
        @SqlQuery("select 1")
        CustomType m();
    }

    @RegisterColumnMapperFactory(CustomTypeColumnMapperFactory.class)
    public interface ClassColumnMapperFactory {
        @SqlQuery("select 1")
        CustomType m();
    }

    public interface MethodColumnMapper {
        @RegisterColumnMapper(CustomTypeColumnMapper.class)
        @SqlQuery("select 1")
        CustomType m();
    }

    public interface MethodColumnMapperFactory {
        @RegisterColumnMapperFactory(CustomTypeColumnMapperFactory.class)
        @SqlQuery("select 1")
        CustomType m();
    }

    public static class CustomTypeColumnMapperFactory implements ColumnMapperFactory {
        @Override
        public Optional<ColumnMapper<?>> build(final Type type, final ConfigRegistry config) {
            if (type == CustomType.class) {
                COLUMN_MAPPER_FACTORY_BUILD.incrementAndGet();
                return Optional.of(new CustomTypeColumnMapper());
            }
            return Optional.empty();
        }
    }

    public static class CustomTypeColumnMapper implements ColumnMapper<CustomType> {
        {
            COLUMN_MAPPER_INIT.incrementAndGet();
        }

        @Override
        public CustomType map(final ResultSet r, final int columnNumber, final StatementContext ctx) throws SQLException {
            return new CustomType();
        }
    }

    @RegisterRowMapper(CustomTypeRowMapper.class)
    public interface ClassRowMapper {
        @SqlQuery("select 1")
        CustomType m();
    }

    @RegisterRowMapperFactory(CustomTypeRowMapperFactory.class)
    public interface ClassRowMapperFactory {
        @SqlQuery("select 1")
        CustomType m();
    }

    public interface MethodRowMapper {
        @RegisterRowMapper(CustomTypeRowMapper.class)
        @SqlQuery("select 1")
        CustomType m();
    }

    public interface MethodRowMapperFactory {
        @RegisterRowMapperFactory(CustomTypeRowMapperFactory.class)
        @SqlQuery("select 1")
        CustomType m();
    }

    public static class CustomTypeRowMapperFactory implements RowMapperFactory {
        @Override
        public Optional<RowMapper<?>> build(final Type type, final ConfigRegistry config) {
            if (type == CustomType.class) {
                ROW_MAPPER_FACTORY_BUILD.incrementAndGet();
                return Optional.of(new CustomTypeRowMapper());
            }
            return Optional.empty();
        }
    }

    public static class CustomTypeRowMapper implements RowMapper<CustomType> {
        {
            ROW_MAPPER_INIT.incrementAndGet();
        }

        @Override
        public CustomType map(final ResultSet r, final StatementContext ctx) throws SQLException {
            return new CustomType();
        }
    }
}
