package org.skife.jdbi.v2.sqlobject;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.*;
import org.skife.jdbi.v2.sqlobject.customizers.FoldWith;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class TestRegisteredFoldersWork
{
    private DBI dbi;
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

    public static class Bean
    {
        private String name;
        private String color;
        private Set<BeanProperty> properties;

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
        public Set<BeanProperty> getProperties()
        {
            return properties;
        }
        public void setProperties(Set<BeanProperty> properties)
        {
            this.properties = properties;
        }
    }

    public static class BeanProperty
    {
        private String attribute;

        public String getAttribute()
        {
            return attribute;
        }

        public void setAttribute(String attribute)
        {
            this.attribute = attribute;
        }
    }

    public static class SillyBean
    {
        private final String name;
        private final String color;
        private final Set<BeanProperty> properties;

        public SillyBean(String name, String color, Set<BeanProperty> properties)
        {
            this.name = name;
            this.color = color;
            this.properties = properties;
        }

        public String getName()
        {
            return name;
        }
        public String getColor()
        {
            return color;
        }
        public Set<BeanProperty> getProperties()
        {
            return properties;
        }
    }

    public static class BeanFolder implements TypedFolder2<Bean> {
        @Override
        public Bean fold(Bean accumulator, ResultSet rs, StatementContext ctx) throws SQLException {
            Bean foldResult = accumulator;
            if (foldResult == null) {
                foldResult = new Bean();
            }
            foldResult.setColor(rs.getString("color"));
            foldResult.setName(rs.getString("name"));
            if (foldResult.getProperties() == null) {
                foldResult.setProperties(new HashSet<BeanProperty>());
            }
            BeanProperty bp = new BeanProperty();
            bp.setAttribute(rs.getString("attribute"));
            foldResult.getProperties().add(bp);
            return foldResult;
        }

        @Override
        public Class<Bean> getAccumulatorType() {
            return Bean.class;
        }
    }

    public static class SillyBeanFolder implements TypedFolder2<SillyBean> {
        @Override
        public SillyBean fold(SillyBean accumulator, ResultSet rs, StatementContext ctx) throws SQLException {
            return accumulator;
        }

        @Override
        public Class<SillyBean> getAccumulatorType() {
            return SillyBean.class;
        }
    }

    public static interface BeanFoldingDao
    {
        @SqlUpdate("create table beans ( name varchar primary key, color varchar )")
        public void createBeanTable();

        @SqlUpdate("create table bean_properties (id bigint primary key auto_increment, name varchar, attribute varchar )")
        public void createBeanPropertyTable();

        @SqlUpdate("insert into beans (name, color) values (:name, :color)")
        public void insertBean(@BindBean Bean bean);

        @SqlBatch("insert into bean_properties (name, attribute) values (:bean.name, :attribute)")
        void insertBeanProperties(@Bind("attribute") Set<String> attributes, @BindBean("bean") Bean bean);

        @SqlQuery("select b.name, b.color, bp.attribute from beans as b right outer join bean_properties bp on b.name = bp.name")
        @FoldWith(BeanFolder.class)
        public Bean findAll();
    }

    @FoldWith(BeanFolder.class)
    public static interface AnotherBeanFoldingDao
    {
        @SqlUpdate("create table beans ( name varchar primary key, color varchar )")
        public void createBeanTable();

        @SqlUpdate("create table bean_properties (id bigint primary key auto_increment, name varchar, attribute varchar )")
        public void createBeanPropertyTable();

        @SqlUpdate("insert into beans (name, color) values (:name, :color)")
        public void insertBean(@BindBean Bean bean);

        @SqlBatch("insert into bean_properties (name, attribute) values (:bean.name, :attribute)")
        void insertBeanProperties(@Bind("attribute") Set<String> attributes, @BindBean("bean") Bean bean);

        @SqlQuery("select b.name, b.color, bp.attribute from beans as b right outer join bean_properties bp on b.name = bp.name")
        public Bean findAll();
    }

    public static interface SillyBeanFoldingDao
    {
        @SqlUpdate("create table beans ( name varchar primary key, color varchar )")
        public void createBeanTable();

        @SqlUpdate("create table bean_properties (id bigint primary key auto_increment, name varchar, attribute varchar )")
        public void createBeanPropertyTable();

        @SqlUpdate("insert into beans (name, color) values (:name, :color)")
        public void insertBean(@BindBean Bean bean);

        @SqlBatch("insert into bean_properties (name, attribute) values (:bean.name, :attribute)")
        void insertBeanProperties(@Bind("attribute") Set<String> attributes, @BindBean("bean") Bean bean);

        @SqlQuery("select b.name, b.color, bp.attribute from beans as b right outer join bean_properties bp on b.name = bp.name")
        @FoldWith(SillyBeanFolder.class)
        public SillyBean findAll();
    }

    @Test
    public void testRegisterMapperAnnotationWorksForMethods() throws Exception
    {
        BeanFoldingDao db = handle.attach(BeanFoldingDao.class);
        db.createBeanTable();
        db.createBeanPropertyTable();

        Bean lima = new Bean();
        lima.setColor("green");
        lima.setName("lima");

        db.insertBean(lima);

        Set<String> limaProperties = new HashSet<String>();
        limaProperties.add("tasty");
        limaProperties.add("large");

        db.insertBeanProperties(limaProperties, lima);

        Bean another_lima = db.findAll();
        assertThat(another_lima.getName(), equalTo(lima.getName()));
        assertThat(another_lima.getColor(), equalTo(lima.getColor()));
        assertEquals(2, another_lima.getProperties().size());
        for (BeanProperty bp: another_lima.getProperties()) {
            assertTrue(limaProperties.contains(bp.getAttribute()));
        }
    }

    @Test
    public void testRegisterMapperAnnotationWorksForTypes() throws Exception
    {
        AnotherBeanFoldingDao db = handle.attach(AnotherBeanFoldingDao.class);
        db.createBeanTable();
        db.createBeanPropertyTable();

        Bean lima = new Bean();
        lima.setColor("green");
        lima.setName("lima");

        db.insertBean(lima);

        Set<String> limaProperties = new HashSet<String>();
        limaProperties.add("tasty");
        limaProperties.add("large");

        db.insertBeanProperties(limaProperties, lima);

        Bean another_lima = db.findAll();
        assertThat(another_lima.getName(), equalTo(lima.getName()));
        assertThat(another_lima.getColor(), equalTo(lima.getColor()));
        assertEquals(2, another_lima.getProperties().size());
        for (BeanProperty bp: another_lima.getProperties()) {
            assertTrue(limaProperties.contains(bp.getAttribute()));
        }
    }

    @Test
    public void testNullInitialAccumulator() throws Exception
    {
        SillyBeanFoldingDao db = handle.attach(SillyBeanFoldingDao.class);
        db.createBeanTable();
        db.createBeanPropertyTable();

        Bean lima = new Bean();
        lima.setColor("green");
        lima.setName("lima");

        db.insertBean(lima);

        Set<String> limaProperties = new HashSet<String>();
        limaProperties.add("tasty");
        limaProperties.add("large");

        db.insertBeanProperties(limaProperties, lima);

        SillyBean another_lima = db.findAll();
        assertNull(another_lima);
    }

}
