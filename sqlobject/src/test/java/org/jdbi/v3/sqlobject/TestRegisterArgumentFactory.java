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
import static org.junit.Assert.assertThat;

import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.ColonPrefixNamedParamStatementRewriter;
import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.jdbi.v3.StatementContext;
import org.jdbi.v3.sqlobject.customizers.RegisterArgumentFactory;
import org.jdbi.v3.tweak.Argument;
import org.jdbi.v3.tweak.ArgumentFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestRegisterArgumentFactory
{
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(String.format("jdbc:h2:mem:%s", UUID.randomUUID()));
        DBI dbi = new DBI(ds);

        // this is the default, but be explicit for sake of clarity in test
        dbi.setStatementRewriter(new ColonPrefixNamedParamStatementRewriter());
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");
    }

    @After
    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }

    @Test
    public void testFoo() throws Exception
    {
        Waffle w = SqlObjectBuilder.attach(handle, Waffle.class);

        w.insert(1, new Name("Brian", "McCallister"));

        assertThat(w.findName(1), equalTo("Brian McCallister"));
    }


    @RegisterArgumentFactory(NameAF.class)
    public interface Waffle
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") Name name);

        @SqlQuery("select name from something where id = :id")
        String findName(@Bind("id") int id);
    }

    public static class NameAF implements ArgumentFactory<Name>
    {
        @Override
        public boolean accepts(Class<?> expectedType, Object value, StatementContext ctx)
        {
            return expectedType == Object.class && value instanceof Name;
        }

        @Override
        public Argument build(Class<?> expectedType, final Name value, StatementContext ctx)
        {
            return (position, statement, ctx1) -> statement.setString(position, value.getFullName());
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
