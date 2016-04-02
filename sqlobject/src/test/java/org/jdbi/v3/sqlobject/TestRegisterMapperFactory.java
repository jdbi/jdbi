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

import static org.jdbi.v3.Types.getErasedType;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.ResultSetMapperFactory;
import org.jdbi.v3.StatementContext;
import org.jdbi.v3.sqlobject.TestRegisterMapperFactory.Foo.FooMapper;
import org.jdbi.v3.sqlobject.customizers.RegisterMapperFactory;
import org.jdbi.v3.tweak.ResultSetMapper;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class TestRegisterMapperFactory
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Test
    public void testSimple() throws Exception
    {
        FooDao fooDao = db.getDbi().onDemand(FooDao.class);

        List<Foo> foos = fooDao.select();
        Assert.assertNotNull(foos);
        Assert.assertEquals(0, foos.size());

        fooDao.insert(1, "John Doe");
        fooDao.insert(2, "Jane Doe");
        List<Foo> foos2 = fooDao.select();
        Assert.assertNotNull(foos2);
        Assert.assertEquals(2, foos2.size());

    }

    @RegisterMapperFactory(MyFactory.class)
    public interface FooDao
    {
        @SqlQuery("select * from something")
        List<Foo> select();

        @SqlUpdate("insert into something (id, name) VALUES (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);
    }


    public static class MyFactory implements ResultSetMapperFactory
    {
        @Override
        public Optional<ResultSetMapper<?>> build(Type type, StatementContext ctx) {
            Class<?> erasedType = getErasedType(type);
            try {
                MapWith mapWith = erasedType.getAnnotation(MapWith.class);
                return mapWith == null
                        ? Optional.empty()
                        : Optional.of(mapWith.value().newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @MapWith(FooMapper.class)
    public static class Foo
    {
        private final int    id;
        private final String name;

        Foo(final int id, final String name)
        {
            this.id = id;
            this.name = name;
        }

        public int getId()
        {
            return id;
        }

        public String getName()
        {
            return name;
        }

        public static class FooMapper implements ResultSetMapper<Foo>
        {
            @Override
            public Foo map(final int index, final ResultSet r, final StatementContext ctx) throws SQLException
            {
                return new Foo(r.getInt("id"), r.getString("name"));
            }
        }
    }
}
