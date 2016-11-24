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
package org.jdbi.v3.core.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.List;

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.junit.Rule;
import org.junit.Test;

public class TestEnums
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

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
        Handle h = db.openHandle();
        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();

        List<SomethingElse> results = h.createQuery("select * from something order by id")
                                   .mapToBean(SomethingElse.class)
                                   .list();
        assertThat(results).extracting(se -> se.name).containsExactly(SomethingElse.Name.eric, SomethingElse.Name.brian);
    }

    @Test
    public void testMapToEnum() throws Exception
    {
        Handle h = db.openHandle();
        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();

        List<SomethingElse.Name> results = h.createQuery("select name from something order by id")
                                   .mapTo(SomethingElse.Name.class)
                                   .list();
        assertThat(results).containsExactly(SomethingElse.Name.eric, SomethingElse.Name.brian);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMapInvalidEnumValue() throws SQLException
    {
        Handle h = db.openHandle();
        h.createUpdate("insert into something (id, name) values (1, 'joe')").execute();

        h.createQuery("select * from something order by id")
                .mapToBean(SomethingElse.class)
                .findFirst();
    }
}
