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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.jdbi.v3.Handle;
import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.Something;
import org.jdbi.v3.StatementContext;
import org.jdbi.v3.sqlobject.customizers.RegisterMapper;
import org.jdbi.v3.sqlobject.helpers.MapResultAsBean;
import org.jdbi.v3.sqlobject.mixins.CloseMe;
import org.jdbi.v3.tweak.ResultSetMapper;
import org.junit.Rule;
import org.junit.Test;

public class TestRegisteredMappersWork
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();


    public interface BooleanDao {
        @SqlQuery("select 1+1 = 2")
        boolean fetchABoolean();
    }

    @Test
    public void testFoo() throws Exception
    {
        boolean world_is_right = SqlObjectBuilder.attach(db.getSharedHandle(), BooleanDao.class).fetchABoolean();
        assertThat(world_is_right, equalTo(true));
    }

    public static class Bean
    {
        private String name;
        private String color;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getColor()
        {
            return color;
        }

        public void setColor(String color)
        {
            this.color = color;
        }
    }

    public interface BeanMappingDao
    {
        @SqlUpdate("create table beans ( name varchar primary key, color varchar )")
        void createBeanTable();

        @SqlUpdate("insert into beans (name, color) values (:name, :color)")
        void insertBean(@BindBean Bean bean);

        @SqlQuery("select name, color from beans where name = :name")
        @MapResultAsBean
        Bean findByName(@Bind("name") String name);
    }

    @Test
    public void testBeanMapperFactory() throws Exception
    {
        BeanMappingDao bdb = SqlObjectBuilder.attach(db.getSharedHandle(), BeanMappingDao.class);
        bdb.createBeanTable();

        Bean lima = new Bean();
        lima.setColor("green");
        lima.setName("lima");

        bdb.insertBean(lima);

        Bean another_lima = bdb.findByName("lima");
        assertThat(another_lima.getName(), equalTo(lima.getName()));
        assertThat(another_lima.getColor(), equalTo(lima.getColor()));
    }

    @Test
    public void testRegistered() throws Exception
    {
        db.getSharedHandle().registerMapper(new SomethingMapper());

        Spiffy s = SqlObjectBuilder.attach(db.getSharedHandle(), Spiffy.class);

        s.insert(1, "Tatu");

        Something t = s.byId(1);
        assertEquals(1, t.getId());
        assertEquals("Tatu", t.getName());
    }

    @Test
    public void testBuiltIn() throws Exception
    {
        Spiffy s = SqlObjectBuilder.attach(db.getSharedHandle(), Spiffy.class);

        s.insert(1, "Tatu");

        assertEquals("Tatu", s.findNameBy(1));
    }

    @Test
    public void testRegisterMapperAnnotationWorks() throws Exception
    {
        Kabob bob = SqlObjectBuilder.onDemand(db.getDbi(), Kabob.class);

        bob.insert(1, "Henning");
        Something henning = bob.find(1);

        assertThat(henning, equalTo(new Something(1, "Henning")));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testNoRootRegistrations() throws Exception
    {
        try (Handle h = db.openHandle()) {
            h.insert("insert into something (id, name) values (1, 'Henning')");
            h.createQuery("select id, name from something where id = 1")
                                 .mapTo(Something.class)
                                 .findFirst();
            fail("should have raised an exception");
        }
    }

    @Test
    public void testNoErrorOnNoData() throws Exception
    {
        Kabob bob = SqlObjectBuilder.onDemand(db.getDbi(), Kabob.class);

        Something henning = bob.find(1);
        assertThat(henning, nullValue());

        List<Something> rs = bob.listAll();
        assertThat(rs.isEmpty(), equalTo(true));

        Iterator<Something> itty = bob.iterateAll();
        assertThat(itty.hasNext(), equalTo(false));
    }

    public interface Spiffy extends CloseMe
    {

        @SqlQuery("select id, name from something where id = :id")
        Something byId(@Bind("id") long id);

        @SqlQuery("select name from something where id = :id")
        String findNameBy(@Bind("id") long id);

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") long id, @Bind("name") String name);
    }


    @RegisterMapper(MySomethingMapper.class)
    public interface Kabob
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something where id = :id")
        Something find(@Bind("id") int id);

        @SqlQuery("select id, name from something order by id")
        List<Something> listAll();

        @SqlQuery("select id, name from something order by id")
        Iterator<Something> iterateAll();
    }

    public static class MySomethingMapper implements ResultSetMapper<Something>
    {
        @Override
        public Something map(int index, ResultSet r, StatementContext ctx) throws SQLException
        {
            return new Something(r.getInt("id"), r.getString("name"));
        }
    }

}
