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
import java.util.List;
import java.util.Optional;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.Unchecked;
import org.jdbi.v3.sqlobject.TestRegisterRowMapperFactory.Foo.FooMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapperFactory;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

public class TestRegisterRowMapperFactory {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Test
    public void testSimple() {
        FooDao fooDao = dbRule.getJdbi().onDemand(FooDao.class);

        List<Foo> foos = fooDao.select();
        assertThat(foos).isEmpty();

        fooDao.insert(1, "John Doe");
        fooDao.insert(2, "Jane Doe");
        List<Foo> foos2 = fooDao.select();
        assertThat(foos2).hasSize(2);

    }

    @RegisterRowMapperFactory(MyFactory.class)
    public interface FooDao {
        @SqlQuery("select * from something")
        List<Foo> select();

        @SqlUpdate("insert into something (id, name) VALUES (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);
    }

    public static class MyFactory implements RowMapperFactory {
        @Override
        public Optional<RowMapper<?>> build(Type type, ConfigRegistry config) {
            Class<?> erasedType = getErasedType(type);
            MapWith mapWith = erasedType.getAnnotation(MapWith.class);
            return mapWith == null
                ? Optional.empty()
                : Optional.of(Unchecked.supplier(mapWith.value()::newInstance).get());
        }
    }

    @MapWith(FooMapper.class)
    public static class Foo {
        private final int id;
        private final String name;

        Foo(final int id, final String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public static class FooMapper implements RowMapper<Foo> {
            @Override
            public Foo map(final ResultSet r, final StatementContext ctx) throws SQLException {
                return new Foo(r.getInt("id"), r.getString("name"));
            }
        }
    }
}
