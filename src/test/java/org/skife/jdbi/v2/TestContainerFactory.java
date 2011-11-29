package org.skife.jdbi.v2;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterContainerMapper;
import org.skife.jdbi.v2.tweak.ContainerFactory;
import org.skife.jdbi.v2.util.StringMapper;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestContainerFactory
{
    private DBI    dbi;
    private Handle h;

    @Before
    public void setUp() throws Exception
    {
        this.dbi = new DBI("jdbc:h2:mem:" + UUID.randomUUID().toString());
        this.dbi.registerContainerFactory(new MaybeContainerFactory());
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


    @RegisterContainerMapper(ImmutableListContainerFactory.class)
    public static interface Dao
    {
        @SqlQuery("select name from something order by id")
        public ImmutableList<String> findAll();

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insert(@BindBean Something it);

    }

    public static class ImmutableListContainerFactory implements ContainerFactory<ImmutableList<?>>
    {

        public boolean accepts(Class<?> type)
        {
            return ImmutableList.class.equals(type);
        }

        public ContainerBuilder<ImmutableList<?>> newContainerBuilderFor(Class<?> type)
        {
            return new ContainerBuilder<ImmutableList<?>>()
            {
                private final ImmutableList.Builder<Object> b =  ImmutableList.builder();

                public ContainerBuilder<ImmutableList<?>> add(Object it)
                {
                    b.add(it);
                    return this;
                }

                public ImmutableList<?> build()
                {
                    return b.build();
                }
            };
        }
    }

    public static class MaybeContainerFactory implements ContainerFactory<Maybe<?>>
    {

        public boolean accepts(Class<?> type)
        {
            return type.equals(Maybe.class);
        }

        public ContainerBuilder<Maybe<?>> newContainerBuilderFor(Class<?> type)
        {
            return new ContainerBuilder<Maybe<?>>()
            {
                private Maybe<Object> value = Maybe.unknown();

                public ContainerBuilder<Maybe<?>> add(Object it)
                {
                    value = value.otherwise(Maybe.definitely(it));
                    return this;
                }

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
