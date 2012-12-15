package org.skife.jdbi.v2;

import java.util.List;

import org.skife.jdbi.derby.Tools;

public class TestEnums extends DBITestCase
{
    private BasicHandle h;
    
    public static class Something {
      public enum Name {
        eric, brian
      }
      
      private int id;
      private Name name;
      public Name getName() {
        return name;
      }
      public void setName(Name name) {
        this.name = name;
      }
      public int getId() {
        return id;
      }
      public void setId(int id) {
        this.id = id;
      }
    }

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        h = openHandle();
    }

    @Override
    public void tearDown() throws Exception
    {
        if (h != null) h.close();
        Tools.stop();
    }

    public void testMapEnumValues() throws Exception
    {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();
        h.createStatement("insert into something (id, name) values (3, NULL)").execute();
        h.createStatement("insert into something (id, name) values (4, '')").execute();
        h.createStatement("insert into something (id, name) values (5, 'a')").execute();

        List<Something> results = h.createQuery("select * from something order by id").map(Something.class).list();
        assertEquals(Something.Name.eric, results.get(0).name);
        assertEquals(Something.Name.brian, results.get(1).name);
        assertNull(results.get(2).name);
        assertNull(results.get(3).name);
        assertNull(results.get(4).name);
    }
}
