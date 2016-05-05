package org.jdbi.v3;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ConstructorMapperTest {

    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Before
    public void setUp() throws Exception {
        db.getSharedHandle().registerRowMapper(ConstructorMapper.factoryFor(ConstructorBean.class));
        db.getSharedHandle().execute("CREATE TABLE bean (s varchar, i integer)");

        db.getSharedHandle().execute("INSERT INTO bean VALUES('3', 2)");
    }


    @Test
    public void testSimple() throws Exception {
        ConstructorBean bean = db.getSharedHandle().createQuery("SELECT s, i FROM bean").mapTo(ConstructorBean.class).findOnly();

        assertEquals("3", bean.s);
        assertEquals(2, bean.i);
    }

    @Test
    public void testReversed() throws Exception {
        ConstructorBean bean = db.getSharedHandle().createQuery("SELECT i, s FROM bean").mapTo(ConstructorBean.class).findOnly();

        assertEquals("3", bean.s);
        assertEquals(2, bean.i);
    }

    @Test(expected = IllegalStateException.class)
    public void testDuplicate() throws Exception {
        ConstructorBean bean = db.getSharedHandle().createQuery("SELECT i, s, s FROM bean").mapTo(ConstructorBean.class).findOnly();

        assertEquals("3", bean.s);
        assertEquals(2, bean.i);
    }

    static class ConstructorBean {
        private final String s;
        private final int i;

        ConstructorBean(String s, int i) {
            this.s = s;
            this.i = i;
        }
    }
}
