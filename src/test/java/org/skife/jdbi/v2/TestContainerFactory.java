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
package org.skife.jdbi.v2;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterContainerMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.skife.jdbi.v2.tweak.ContainerFactory;
import org.skife.jdbi.v2.util.StringMapper;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestContainerFactory
{
    private DBI    dbi;
    private Handle h;

    @Before
    public void setUp() throws Exception
    {
        this.dbi = new DBI("jdbc:h2:mem:" + UUID.randomUUID());
        this.h = dbi.open();
        h.execute("create table something (id int primary key, name varchar)");
    }

    @After
    public void tearDown() throws Exception
    {
        h.close();
    }

    @Test
    public void testExists() throws Exception
    {
        h.execute("insert into something (id, name) values (1, 'Coda')");
        h.registerContainerFactory(new MaybeContainerFactory());

        Maybe<String> rs = h.createQuery("select name from something where id = :id")
                            .bind("id", 1)
                            .map(StringMapper.FIRST)
                            .first(Maybe.class);

        assertThat(rs.isKnown(), equalTo(true));
        assertThat(rs.getValue(), equalTo("Coda"));
    }

    @Test
    public void testDoesNotExist() throws Exception
    {
        h.execute("insert into something (id, name) values (1, 'Coda')");
        h.registerContainerFactory(new MaybeContainerFactory());

        Maybe<String> rs = h.createQuery("select name from something where id = :id")
                            .bind("id", 2)
                            .map(StringMapper.FIRST)
                            .first(Maybe.class);

        assertThat(rs.isKnown(), equalTo(false));
    }

    @Test
    public void testOnList() throws Exception
    {
        h.registerContainerFactory(new ImmutableListContainerFactory());

        h.execute("insert into something (id, name) values (1, 'Coda')");
        h.execute("insert into something (id, name) values (2, 'Brian')");

        ImmutableList<String> rs = h.createQuery("select name from something order by id")
                                    .map(StringMapper.FIRST)
                                    .list(ImmutableList.class);

        assertThat(rs, equalTo(ImmutableList.of("Coda", "Brian")));
    }

    @Test
    public void testWithSqlObject() throws Exception
    {
        Dao dao = dbi.onDemand(Dao.class);
        dao.insert(new Something(1, "Coda"));
        dao.insert(new Something(2, "Brian"));

        ImmutableList<String> rs = dao.findAll();
        assertThat(rs, equalTo(ImmutableList.of("Coda", "Brian")));
    }

    @Test
    public void testWithSqlObjectSingleValue() throws Exception
    {
        Dao dao = dbi.onDemand(Dao.class);
        dao.insert(new Something(1, "Coda"));
        dao.insert(new Something(2, "Brian"));

        Maybe<String> rs = dao.findNameById(1);
        assertThat(rs, equalTo(Maybe.definitely("Coda")));

        rs = dao.smartFindNameById(1);
        assertThat(rs, equalTo(Maybe.definitely("Coda")));

        rs = dao.inheritedGenericFindNameById(1);
        assertThat(rs, equalTo(Maybe.definitely("Coda")));
    }

    @Test
    public void testWithSqlObjectSetReturnValue() throws Exception
    {
        Dao dao = dbi.onDemand(Dao.class);
        dao.insert(new Something(1, "Coda"));
        dao.insert(new Something(2, "Brian"));

        Set<String> rs = dao.findAllAsSet();
        assertThat(rs, equalTo((Set<String>)ImmutableSet.of("Coda", "Brian")));
    }


    @RegisterContainerMapper({ImmutableListContainerFactory.class, MaybeContainerFactory.class})
    public static interface Dao extends Base<String>
    {
        @SqlQuery("select name from something order by id")
        public ImmutableList<String> findAll();

        @SqlQuery("select name from something order by id")
        public Set<String> findAllAsSet();

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insert(@BindBean Something it);

        @SqlQuery("select name from something where id = :id")
        @SingleValueResult(String.class)
        public Maybe<String> findNameById(@Bind("id") int id);

        @SqlQuery("select name from something where id = :id")
        @SingleValueResult
        public Maybe<String> smartFindNameById(@Bind("id") int id);
    }

    public static interface Base<T>
    {
        @SqlQuery("select name from something where id = :id")
        @SingleValueResult
        public Maybe<T> inheritedGenericFindNameById(@Bind("id") int id);
    }


    public static class SetContainerFactory implements ContainerFactory<Set<?>>
    {

        @Override
        public boolean accepts(Class<?> type)
        {
            return Set.class.equals(type);
        }

        @Override
        public ContainerBuilder<Set<?>> newContainerBuilderFor(Class<?> type)
        {
            return new ContainerBuilder<Set<?>>()
            {
                private Set<Object> rs = new LinkedHashSet<Object>();

                @Override
                public ContainerBuilder<Set<?>> add(Object it)
                {
                    rs.add(it);
                    return this;
                }

                @Override
                public Set<?> build()
                {
                    return rs;
                }
            };
        }
    }

    public static class ImmutableListContainerFactory implements ContainerFactory<ImmutableList<?>>
    {

        @Override
        public boolean accepts(Class<?> type)
        {
            return ImmutableList.class.equals(type);
        }

        @Override
        public ContainerBuilder<ImmutableList<?>> newContainerBuilderFor(Class<?> type)
        {
            return new ContainerBuilder<ImmutableList<?>>()
            {
                private final ImmutableList.Builder<Object> b = ImmutableList.builder();

                @Override
                public ContainerBuilder<ImmutableList<?>> add(Object it)
                {
                    b.add(it);
                    return this;
                }

                @Override
                public ImmutableList<?> build()
                {
                    return b.build();
                }
            };
        }
    }

    public static class MaybeContainerFactory implements ContainerFactory<Maybe<?>>
    {

        @Override
        public boolean accepts(Class<?> type)
        {
            return type.equals(Maybe.class);
        }

        @Override
        public ContainerBuilder<Maybe<?>> newContainerBuilderFor(Class<?> type)
        {
            return new ContainerBuilder<Maybe<?>>()
            {
                private Maybe<Object> value = Maybe.unknown();

                @Override
                public ContainerBuilder<Maybe<?>> add(Object it)
                {
                    value = value.otherwise(Maybe.definitely(it));
                    return this;
                }

                @Override
                public Maybe<?> build()
                {
                    return value;
                }
            };
        }
    }

    public static abstract class Maybe<T>
    {

        public abstract Maybe<T> otherwise(Maybe<T> maybeDefaultValue);

        public abstract T getValue();

        public abstract boolean isKnown();

        public static <T> Maybe<T> definitely(final T theValue)
        {
            return new DefiniteValue<T>(theValue);
        }


        public static <T> Maybe<T> unknown()
        {
            return new Maybe<T>()
            {
                @Override
                public boolean isKnown()
                {
                    return false;
                }

                @Override
                public Maybe<T> otherwise(Maybe<T> maybeDefaultValue)
                {
                    return maybeDefaultValue;
                }

                @Override
                public T getValue()
                {
                    throw new IllegalStateException("No value known!");
                }

                @Override
                public String toString()
                {
                    return "unknown";
                }

                @Override
                @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
                public boolean equals(Object obj)
                {
                    return false;
                }

                @Override
                public int hashCode()
                {
                    return 0;
                }
            };
        }

        private static class DefiniteValue<T> extends Maybe<T>
        {
            private final T theValue;

            public DefiniteValue(T theValue)
            {
                this.theValue = theValue;
            }

            @Override
            public boolean isKnown()
            {
                return true;
            }

            @Override
            public Maybe<T> otherwise(Maybe<T> maybeDefaultValue)
            {
                return this;
            }

            @Override
            public T getValue()
            {
                return theValue;
            }

            @Override
            public String toString()
            {
                return "definitely " + theValue.toString();
            }

            @Override
            public boolean equals(Object o)
            {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                DefiniteValue that = (DefiniteValue) o;
                return theValue.equals(that.theValue);
            }

            @Override
            public int hashCode()
            {
                return theValue.hashCode();
            }
        }
    }

}
