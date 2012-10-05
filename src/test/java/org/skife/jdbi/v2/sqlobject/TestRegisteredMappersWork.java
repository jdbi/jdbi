package org.skife.jdbi.v2.sqlobject;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.exceptions.DBIException;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.helpers.MapResultAsBean;
import org.skife.jdbi.v2.sqlobject.mixins.CloseMe;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.util.NamingStrategy;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

public class TestRegisteredMappersWork
{
    private DBI    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
        dbi = new DBI(ds);
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");
    }

    @After
    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }


    public static interface BooleanDao {
        @SqlQuery("select 1+1 = 2")
        public boolean fetchABoolean();
    }

    @Test
    public void testFoo() throws Exception
    {
        boolean world_is_right = handle.attach(BooleanDao.class).fetchABoolean();
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

    public static interface BeanMappingDao
    {
        @SqlUpdate("create table beans ( name varchar primary key, color varchar )")
        public void createBeanTable();

        @SqlUpdate("insert into beans (name, color) values (:name, :color)")
        public void insertBean(@BindBean Bean bean);

        @SqlQuery("select name, color from beans where name = :name")
        @MapResultAsBean
        public Bean findByName(@Bind("name") String name);
    }

    @Test
    public void testBeanMapperFactory() throws Exception
    {
        BeanMappingDao db = handle.attach(BeanMappingDao.class);
        db.createBeanTable();

        Bean lima = new Bean();
        lima.setColor("green");
        lima.setName("lima");

        db.insertBean(lima);

        Bean another_lima = db.findByName("lima");
        assertThat(another_lima.getName(), equalTo(lima.getName()));
        assertThat(another_lima.getColor(), equalTo(lima.getColor()));
    }

    public static class User
    {
        private Integer userId;
        private String username;

        public Integer getUserId()
        {
           return userId;
        }

        public void setUserId(Integer userId)
        {
           this.userId = userId;
        }

        public String getUsername()
        {
           return username;
        }

        public void setUsername(String username)
        {
           this.username = username;
        }
    }

    public static interface UserBeanMappingDao
    {
        @SqlUpdate("create table users ( user_id int primary key, username varchar )")
        public void createUserTable();

        @SqlUpdate("insert into users (user_id, username) values (:userId, :username)")
        public void insertUser(@BindBean User user);

        @SqlQuery("select user_id, username from users where username = :username")
        @MapResultAsBean(propertyFormattingStrategy = NamingStrategy.UPPER_UNDERSCORE, dbFormattingStrategy = NamingStrategy.IDENTICAL)
        public User findByName(@Bind("username") String username);
    }

    @Test
    public void testSnakeCaseUserMapperFactory() throws Exception
    {
        UserBeanMappingDao db = handle.attach(UserBeanMappingDao.class);
        db.createUserTable();

        User user = new User();
        user.setUserId(1);
        user.setUsername("Galatasaray");

        db.insertUser(user);

        User another_user = db.findByName("Galatasaray");
        assertThat(another_user.getUsername(), equalTo("Galatasaray"));
        assertThat(another_user.getUserId(), equalTo(1));
    }

    @Test
    public void testRegistered() throws Exception
    {
        handle.registerMapper(new SomethingMapper());

        Spiffy s = SqlObjectBuilder.attach(handle, Spiffy.class);

        s.insert(1, "Tatu");

        Something t = s.byId(1);
        assertEquals(1, t.getId());
        assertEquals("Tatu", t.getName());
    }

    @Test
    public void testBuiltIn() throws Exception
    {

        Spiffy s = SqlObjectBuilder.attach(handle, Spiffy.class);

        s.insert(1, "Tatu");

        assertEquals("Tatu", s.findNameBy(1));
    }

    @Test
    public void testRegisterMapperAnnotationWorks() throws Exception
    {
        Kabob bob = dbi.onDemand(Kabob.class);

        bob.insert(1, "Henning");
        Something henning = bob.find(1);

        assertThat(henning, equalTo(new Something(1, "Henning")));
    }

    @Test(expected = DBIException.class)
    public void testNoRootRegistrations() throws Exception
    {
        Handle h = dbi.open();
        h.insert("insert into something (id, name) values (1, 'Henning')");
        try {
            Something henning = h.createQuery("select id, name from something where id = 1")
                                 .mapTo(Something.class)
                                 .first();
            fail("should have raised an exception");
        }
        finally {
            h.close();
        }
    }

    @Test
    public void testNoErrorOnNoData() throws Exception
    {
        Kabob bob = dbi.onDemand(Kabob.class);

        Something henning = bob.find(1);
        assertThat(henning, nullValue());

        List<Something> rs = bob.listAll();
        assertThat(rs.isEmpty(), equalTo(true));

        Iterator<Something> itty = bob.iterateAll();
        assertThat(itty.hasNext(), equalTo(false));
    }

    public static interface Spiffy extends CloseMe
    {

        @SqlQuery("select id, name from something where id = :id")
        public Something byId(@Bind("id") long id);

        @SqlQuery("select name from something where id = :id")
        public String findNameBy(@Bind("id") long id);

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insert(@Bind("id") long id, @Bind("name") String name);
    }


    @RegisterMapper(MySomethingMapper.class)
    public static interface Kabob
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something where id = :id")
        public Something find(@Bind("id") int id);

        @SqlQuery("select id, name from something order by id")
        public List<Something> listAll();

        @SqlQuery("select id, name from something order by id")
        public Iterator<Something> iterateAll();
    }

    public static class MySomethingMapper implements ResultSetMapper<Something>
    {
        public Something map(int index, ResultSet r, StatementContext ctx) throws SQLException
        {
            return new Something(r.getInt("id"), r.getString("name"));
        }
    }

}
