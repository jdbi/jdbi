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

import org.junit.Test;

import java.sql.SQLException;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

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

    @Test
    public void testMapEnumValues() throws Exception
    {
        Handle h = openHandle();
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();

        List<SomethingElse> results = h.createQuery("select * from something order by id")
                                   .map(SomethingElse.class)
                                   .list();
        assertEquals(SomethingElse.Name.eric, results.get(0).name);
        assertEquals(SomethingElse.Name.brian, results.get(1).name);
    }

    @Test
    public void testMapToEnum() throws Exception
    {
        Handle h = openHandle();
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();

        List<SomethingElse.Name> results = h.createQuery("select name from something order by id")
                                   .mapTo(SomethingElse.Name.class)
                                   .list();
        assertEquals(SomethingElse.Name.eric, results.get(0));
        assertEquals(SomethingElse.Name.brian, results.get(1));
    }

    @Test
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
