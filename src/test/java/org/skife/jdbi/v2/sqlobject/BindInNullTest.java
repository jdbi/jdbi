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
import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.logging.FormattedLog;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.tweak.SQLLog;
import org.skife.jdbi.v2.unstable.BindIn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.skife.jdbi.v2.unstable.BindIn.EmptyHandling.NULL;
import static org.skife.jdbi.v2.unstable.BindIn.EmptyHandling.VOID;

public class BindInNullTest
{
    private static Handle handle;

    @BeforeClass
    public static void init()
    {
        final JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        final DBI dbi = new DBI(ds);
        dbi.registerMapper(new SomethingMapper());
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");
        handle.execute("insert into something(id, name) values(1, null)");
        handle.execute("insert into something(id, name) values(2, null)");
        handle.execute("insert into something(id, name) values(3, null)");
        handle.execute("insert into something(id, name) values(4, null)");

        handle.execute("insert into something(id, name) values(5, 'bla')");
        handle.execute("insert into something(id, name) values(6, 'null')");
        handle.execute("insert into something(id, name) values(7, '')");
    }

    @AfterClass
    public static void exit()
    {
        handle.execute("drop table something");
        handle.close();
    }

    @Test
    public void testSomethingByIterableHandleNullWithNull()
    {
        final SomethingByIterableHandleNull s = handle.attach(SomethingByIterableHandleNull.class);

        final List<Something> out = s.get(null);

        Assert.assertEquals(0, out.size());
    }

    @Test
    public void testSomethingByIterableHandleNullWithEmptyList()
    {
        final SomethingByIterableHandleNull s = handle.attach(SomethingByIterableHandleNull.class);

        final List<Something> out = s.get(new ArrayList<Object>());

        Assert.assertEquals(0, out.size());
    }

    @UseStringTemplate3StatementLocator
    private interface SomethingByIterableHandleNull
    {
        @SqlQuery("select id, name from something where name in (<names>)")
        List<Something> get(@BindIn(value = "names", onEmpty = NULL) Iterable<Object> ids);
    }

    //

    @Test
    public void testSomethingByIterableHandleVoidWithNull()
    {
        final TestSqlLogger logger = new TestSqlLogger();
        handle.setSQLLog(logger);
        final SomethingByIterableHandleVoid s = handle.attach(SomethingByIterableHandleVoid.class);

        final List<Something> out = s.get(null);
        final List<String> log = logger.getLog();

        Assert.assertEquals(0, out.size());
        Assert.assertEquals(1, log.size());
        Assert.assertThat(log.get(0), CoreMatchers.containsString(" where id in ();"));
    }

    @Test
    public void testSomethingByIterableHandleVoidWithEmptyList()
    {
        final TestSqlLogger logger = new TestSqlLogger();
        handle.setSQLLog(logger);
        final SomethingByIterableHandleVoid s = handle.attach(SomethingByIterableHandleVoid.class);

        final List<Something> out = s.get(new ArrayList<Object>());
        final List<String> log = logger.getLog();

        Assert.assertEquals(0, out.size());
        Assert.assertEquals(1, log.size());
        Assert.assertThat(log.get(0), CoreMatchers.containsString(" where id in ();"));
    }

    @UseStringTemplate3StatementLocator
    private interface SomethingByIterableHandleVoid
    {
        @SqlQuery("select id, name from something where id in (<ids>);")
        List<Something> get(@BindIn(value = "ids", onEmpty = VOID) Iterable<Object> ids);
    }

    private class TestSqlLogger extends FormattedLog implements SQLLog
    {
        final List<String> log = new ArrayList<String>();

        @Override
        protected boolean isEnabled()
        {
            return true;
        }

        @Override
        protected void log(final String msg)
        {
            log.add(msg);
        }

        public List<String> getLog()
        {
            // make a copy just to guarantee integrity
            return new ArrayList<String>(log);
        }
    }
}