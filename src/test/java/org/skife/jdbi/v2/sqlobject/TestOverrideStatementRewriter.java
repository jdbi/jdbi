/*
 * Copyright 2004 - 2011 Brian McCallister
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.skife.jdbi.v2.sqlobject;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.ColonPrefixNamedParamStatementRewriter;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.HashPrefixStatementRewriter;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.sqlobject.binders.Bind;
import org.skife.jdbi.v2.sqlobject.binders.BindBean;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestOverrideStatementRewriter
{
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
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
        // test will raise exceptions if SQL is bogus -- if it uses the colon prefix form

        Hashed h = handle.attach(Hashed.class);
        h.insert(new Something(1, "Joy"));
        Something s = h.findById(1);
        assertThat(s.getName(), equalTo("Joy"));
    }


    @OverrideStatementRewriterWith(HashPrefixStatementRewriter.class)
    @RegisterMapper(SomethingMapper.class)
    static interface Hashed
    {
        @SqlUpdate("insert into something (id, name) values (#id, #name)")
        public void insert(@BindBean Something s);

        @SqlQuery("select id, name from something where id = #id")
        public Something findById(@Bind("id") int id);

    }

}
