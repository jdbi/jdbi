package org.skife.jdbi.v2;

import java.sql.SQLException;
import java.util.List;

public class TestEnums extends DBITestCase
{

    public static class SomethingElse
    {
        public enum Name
        {
            eric, brian
        }

        private int id;
        private Name name;

        public Name getName()
        {
            return name;
        }

        public void setName(Name name)
        {
            this.name = name;
        }

        public int getId()
        {
            return id;
        }

        public void setId(int id)
        {
            this.id = id;
        }
    }

    public void testMapEnumValues() throws Exception
    {
        Handle h = openHandle();
        h.createStatement("insert into something (id, name) values (1, ?)")
                .bind(0, SomethingElse.Name.eric)
                .execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();

        List<SomethingElse> results = h.createQuery("select * from something order by id")
                                   .map(SomethingElse.class)
                                   .list();
        assertEquals(SomethingElse.Name.eric, results.get(0).name);
        assertEquals(SomethingElse.Name.brian, results.get(1).name);
    }

    public void testMapInvalidEnumValue() throws SQLException
    {
        Handle h = openHandle();
        h.createStatement("insert into something (id, name) values (1, 'joe')").execute();

        try {
            h.createQuery("select * from something order by id")
             .map(SomethingElse.class)
             .first();
            fail("Expected IllegalArgumentException was not thrown");
        }
        catch (IllegalArgumentException e) {
            assertEquals("flow control goes here", 2 + 2, 4);
        }


    }
}
