/* Copyright 2004-2005 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi;

import junit.framework.TestCase;
import org.skife.jdbi.derby.Tools;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

public class TestBatch extends TestCase
{
    private Handle handle;

    public void setUp() throws Exception
    {
        Tools.start();
        Tools.dropAndCreateSomething();
        handle = DBI.open(Tools.CONN_STRING);
    }

    public void tearDown() throws Exception
    {
        if (handle.isOpen()) handle.close();
        Tools.stop();
    }

    public void testBatchOfOne() throws Exception
    {
        final Batch b = handle.batch();
        b.add("insert into something (id, name) values (1, 'one')");
        b.execute();

        final Collection results = handle.query("select id, name from something order by id");
        assertEquals(1, results.size());
    }

    public void testBatchOfTwo() throws Exception
    {
        handle.batch()
                .add("insert into something (id, name) values (1, 'one')")
                .add("insert into something (id, name) values (2, 'two')")
                .execute();

        final Collection results = handle.query("select id, name from something order by id");
        assertEquals(2, results.size());
    }

    public void testPreparedPositionalBatch() throws Exception
    {
        handle.prepareBatch("insert into something (id, name) values (?, ?)")
                .add(new Object[]{new Integer(1), "one"})
                .add(new Object[]{new Integer(2), "two"})
                .execute();
        final Collection results = handle.query("select id, name from something order by id");
        assertEquals(2, results.size());
    }

    public void testBeanPropertyBatch() throws Exception
    {
        handle.prepareBatch("insert into something (id, name) values (:id, :name)")
                .add(new Something(1, "one"))
                .add(new Something(2, "two"))
                .execute();
        final Collection results = handle.query("select id, name from something order by id");
        assertEquals(2, results.size());
    }

    public void testPreparedWithMap() throws Exception
    {
        handle.prepareBatch("insert into something (id, name) values (:id, :name)")
                .add(Args.with("id", new BigDecimal(1)).and("name", "one"))
                .add(Args.with("id", new BigDecimal(2)).and("name", "two"))
                .execute();
        final Collection results = handle.query("select id, name from something order by id");
        assertEquals(2, results.size());
    }

    public void testMixedAddAll() throws Exception
    {
        final Collection bunch = new ArrayList();
        bunch.add(Args.with("id", new BigDecimal(1)).and("name", "one"));
        bunch.add(new Something(2, "two"));

        handle.prepareBatch("insert into something (id, name) values (:id, :name)")
                .addAll(bunch)
                .execute();
        final Collection results = handle.query("select id, name from something order by id");
        assertEquals(2, results.size());
    }

    public void testPreparedBatch() throws Exception
    {
        handle.prepareBatch("insert into something (id, name) values (:id, :name)")
                .add(new Something(1, "one"))
                .add(new Something(2, "two"))
                .add(new Something(3, "three"))
                .add(new Something(4, "four"))
                .execute();
        assertEquals(4, handle.query("select * from something").size());
    }

    public void testPreparedBatch2() throws Exception
    {
        final Something[] things = new Something[]
        {
            new Something(1, "one"),
            new Something(2, "two"),
            new Something(3, "three"),
            new Something(4, "four")
        };
        handle.prepareBatch("insert into something (id, name) values (:id, :name)")
                .addAll(things)
                .execute();
        assertEquals(4, handle.query("select * from something").size());
    }
}
