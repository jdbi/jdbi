/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
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
package org.skife.jdbi.v2.sqlobject;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.ColonPrefixNamedParamStatementRewriter;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterArgumentFactory;
import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.ArgumentFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

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
        Waffle w = handle.attach(Waffle.class);

        w.insert(1, new Name("Brian", "McCallister"));

        assertThat(w.findName(1), equalTo("Brian McCallister"));
    }


    @RegisterArgumentFactory(NameAF.class)
    public static interface Waffle
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") Name name);

        @SqlQuery("select name from something where id = :id")
        String findName(@Bind("id") int id);
    }

    public static class NameAF implements ArgumentFactory<Name>
    {
        public boolean accepts(Class<?> expectedType, Object value, StatementContext ctx)
        {
            return expectedType == Object.class && value instanceof Name;
        }

        public Argument build(Class<?> expectedType, final Name value, StatementContext ctx)
        {
            return new Argument()
            {
                public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException
                {
                    statement.setString(position, value.getFullName());
                }
            };
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

        public String toString()
        {
            return "<Name first=" + first + " last=" + last + " >";
        }
    }

}
