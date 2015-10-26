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
package org.jdbi.v3;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.jdbi.v3.tweak.Argument;
import org.jdbi.v3.tweak.ArgumentFactory;
import org.junit.Rule;
import org.junit.Test;

public class TestArgumentFactory
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Test
    public void testRegisterOnDBI() throws Exception
    {
        final DBI dbi = db.getDbi();
        dbi.registerArgumentFactory(new NameAF());
        try (Handle h = dbi.open()) {
            h.createStatement("insert into something (id, name) values (:id, :name)")
              .bind("id", 7)
              .bind("name", new Name("Brian", "McCallister"))
              .execute();

            String full_name = h.createQuery("select name from something where id = 7").mapTo(String.class).findOnly();

            assertThat(full_name, equalTo("Brian McCallister"));
        }
    }

    @Test
    public void testRegisterOnHandle() throws Exception
    {
        try (Handle h = db.openHandle()) {
            h.registerArgumentFactory(new NameAF());
            h.createStatement("insert into something (id, name) values (:id, :name)")
             .bind("id", 7)
             .bind("name", new Name("Brian", "McCallister"))
             .execute();

            String full_name = h.createQuery("select name from something where id = 7").mapTo(String.class).findOnly();

            assertThat(full_name, equalTo("Brian McCallister"));
        }
    }

    @Test
    public void testRegisterOnStatement() throws Exception
    {
        db.getSharedHandle().createStatement("insert into something (id, name) values (:id, :name)")
         .registerArgumentFactory(new NameAF())
         .bind("id", 1)
         .bind("name", new Name("Brian", "McCallister"))
         .execute();
    }

    @Test
    public void testOnPreparedBatch() throws Exception
    {
        Handle h = db.getSharedHandle();
        PreparedBatch batch = h.prepareBatch("insert into something (id, name) values (:id, :name)");
        batch.registerArgumentFactory(new NameAF());

        batch.add().bind("id", 1).bind("name", new Name("Brian", "McCallister"));
        batch.add().bind("id", 2).bind("name", new Name("Henning", "S"));
        batch.execute();

        List<String> rs = h.createQuery("select name from something order by id")
                           .mapTo(String.class)
                           .list();

        assertThat(rs.get(0), equalTo("Brian McCallister"));
        assertThat(rs.get(1), equalTo("Henning S"));
    }

    public static class NameAF implements ArgumentFactory<Name>
    {
        @Override
        public boolean accepts(Class<?> expectedType, Object value, StatementContext ctx)
        {
            return expectedType == Object.class && value instanceof Name;
        }

        @Override
        public Argument build(Class<?> expectedType, Name value, StatementContext ctx)
        {
            return new StringArgument(value.getFullName());
        }
    }

    public static class Name
    {
        private final String first;
        private final String last;

        public Name(String first, String last)
        {
            this.first = first;
            this.last = last;
        }

        public String getFullName()
        {
            return first + " " + last;
        }

        @Override
        public String toString()
        {
            return "<Name first=" + first + " last=" + last + " >";
        }
    }

}
