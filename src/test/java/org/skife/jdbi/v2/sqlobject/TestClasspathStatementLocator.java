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
package org.skife.jdbi.v2.sqlobject;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestClasspathStatementLocator {
    private Handle handle;

    @Before
    public void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        DBI dbi = new DBI(ds);
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");
        handle.execute("insert into something (id, name) values (6, 'Martin')");
    }

    @After
    public void tearDown() throws Exception {
        handle.execute("drop table something");
        handle.close();
    }

    @Test
    public void testBam() throws Exception {
        Something s = handle.attach(Cromulence.class).findById(6L);
        assertThat(s.getName(), equalTo("Martin"));
    }

    @Test
    public void testOverride() throws Exception {
        Something s = handle.attach(SubCromulence.class).findById(6L);
        assertThat(s.getName(), equalTo("overridden"));
    }

    @Test
    public void testCachedOverride() throws Exception {
        Something s = handle.attach(Cromulence.class).findById(6L);
        assertThat(s.getName(), equalTo("Martin"));

        // and now make sure we don't accidentally cache the statement from above
        s = handle.attach(SubCromulence.class).findById(6L);
        assertThat(s.getName(), equalTo("overridden"));
    }

    @RegisterMapper(SomethingMapper.class)
    static interface Cromulence {
        @SqlQuery
        public Something findById(@Bind("id") Long id);
    }

    @RegisterMapper(SomethingMapper.class)
    static interface SubCromulence extends Cromulence { }
}
