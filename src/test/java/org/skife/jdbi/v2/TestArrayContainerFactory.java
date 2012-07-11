package org.skife.jdbi.v2;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.ContainerValueResult;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterContainerMapper;
import org.skife.jdbi.v2.tweak.ContainerFactory;
import org.skife.jdbi.v2.util.StringMapper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestArrayContainerFactory
{
    private DBI dbi;
    private Handle h;

    @Before
    public void setUp() throws Exception
    {
        this.dbi = new DBI("jdbc:h2:mem:" + UUID.randomUUID().toString());
        this.h = dbi.open();
        h.execute("create table something (id int primary key, name varchar)");
    }

    @After
    public void tearDown() throws Exception
    {
        h.close();
    }

    @Test
    public void testOnList() throws Exception
    {
        h.registerContainerFactory(new ArrayContainerFactory());

        h.execute("insert into something (id, name) values (1, 'Coda')");
        h.execute("insert into something (id, name) values (2, 'Brian')");

        String[] rs = h.createQuery("select name from something order by id")
                .map(StringMapper.FIRST)
                .list(String[].class);

        assertThat(rs, equalTo(new String[]{"Coda", "Brian"}));
    }

    @Test
    public void testWithSqlObject() throws Exception
    {
        Dao dao = dbi.onDemand(Dao.class);
        dao.insert(new Something(1, "Coda"));
        dao.insert(new Something(2, "Brian"));

        String[] rs = dao.findAll();
        assertThat(rs, equalTo(new String[]{"Coda", "Brian"}));
    }

    @RegisterContainerMapper(ArrayContainerFactory.class)
    public static interface Dao
    {
        @SqlQuery("select name from something order by id")
        @ContainerValueResult(String.class)
        public String[] findAll();

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insert(@BindBean Something it);
    }

    public static class ArrayContainerFactory implements ContainerFactory<Object> {

        public boolean accepts(Class<?> type) {
            return type.isArray();
        }

        public ContainerBuilder<Object> newContainerBuilderFor(final Class<?> type) {
            return new ContainerBuilder<Object>() {
                private final ArrayList<Object> list = new ArrayList<Object>();

                public ContainerBuilder<Object> add(Object it) {
                    list.add(it);
                    return this;
                }

                public Object build() {
                    Object result = Array.newInstance(type.getComponentType(), list.size());
                    for (int i = 0; i < list.size(); ++i) {
                        Array.set(result, i, list.get(i));
                    }
                    return result;
                }
            };
        }

    }
}
