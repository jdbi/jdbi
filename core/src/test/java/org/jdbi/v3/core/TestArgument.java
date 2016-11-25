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
package org.jdbi.v3.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.jdbi.v3.core.argument.Argument;
import org.junit.Rule;
import org.junit.Test;

public class TestArgument
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Test
    public void testRegisterOnDBI() throws Exception
    {
        final Jdbi dbi = db.getJdbi();
        dbi.registerArgument(new NameArgument());
        try (Handle h = dbi.open()) {
            h.createUpdate("insert into something (id, name) values (:id, :name)")
              .bind("id", 7)
              .bind("name", new Name("Brian", "McCallister"))
              .execute();

            String full_name = h.createQuery("select name from something where id = 7").mapTo(String.class).findOnly();

            assertThat(full_name).isEqualTo("Brian McCallister");
        }
    }

    @Test
    public void testRegisterOnHandle() throws Exception
    {
        try (Handle h = db.openHandle()) {
            h.registerArgument(new NameArgument());
            h.createUpdate("insert into something (id, name) values (:id, :name)")
             .bind("id", 7)
             .bind("name", new Name("Brian", "McCallister"))
             .execute();

            String full_name = h.createQuery("select name from something where id = 7").mapTo(String.class).findOnly();

            assertThat(full_name).isEqualTo("Brian McCallister");
        }
    }

    @Test
    public void testRegisterOnStatement() throws Exception
    {
        db.getSharedHandle().createUpdate("insert into something (id, name) values (:id, :name)")
         .registerArgument(new NameArgument())
         .bind("id", 1)
         .bind("name", new Name("Brian", "McCallister"))
         .execute();
    }

    @Test
    public void testOnPreparedBatch() throws Exception
    {
        Handle h = db.getSharedHandle();
        PreparedBatch batch = h.prepareBatch("insert into something (id, name) values (:id, :name)");
        batch.registerArgument(new NameArgument());

        batch.add().bind("id", 1).bind("name", new Name("Brian", "McCallister"));
        batch.add().bind("id", 2).bind("name", new Name("Henning", "S"));
        batch.execute();

        List<String> rs = h.createQuery("select name from something order by id")
                           .mapTo(String.class)
                           .list();

        assertThat(rs.get(0)).isEqualTo("Brian McCallister");
        assertThat(rs.get(1)).isEqualTo("Henning S");
    }

    public static class NameArgument implements Argument<Name> {
        @Override
        public void apply(PreparedStatement statement, int position, Name value, StatementContext ctx) throws SQLException {
            statement.setString(position, value.getFullName());
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
