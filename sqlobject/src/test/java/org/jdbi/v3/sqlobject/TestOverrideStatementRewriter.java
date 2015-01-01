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
import org.jdbi.v3.HashPrefixStatementRewriter;
import org.jdbi.v3.Something;
import org.jdbi.v3.sqlobject.customizers.OverrideStatementRewriterWith;
import org.jdbi.v3.sqlobject.customizers.RegisterMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestOverrideStatementRewriter
{
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
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

        Hashed h = SqlObjectBuilder.attach(handle, Hashed.class);
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
