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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class TestInExpressions extends TestCase
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
        handle.close();
        Tools.stop();
    }

    public void testNothing() throws Exception
    {

    }

    public void _testInCollection() throws Exception
    {
        handle.prepareBatch("insert into something (id, name) values (:id, :name)")
                .add(new Something(1, "one"))
                .add(new Something(2, "two"))
                .add(new Something(3, "three"))
                .add(new Something(4, "four"))
                .execute();

        final Collection ids = new HashSet();
        ids.add(new Integer(1));
        ids.add(new Integer(2));

        final List things = handle.query("select * from something where id in (:ids) order by id",
                                         Args.with("ids", ids));
        assertEquals(2, things.size());
    }

    public void _testInArray() throws Exception
    {
        handle.prepareBatch("insert into something (id, name) values (:id, :name)")
                .add(new Something(1, "one"))
                .add(new Something(2, "two"))
                .add(new Something(3, "three"))
                .add(new Something(4, "four"))
                .execute();

        final List things = handle.query("select * from something where id in (:ids) order by id",
                                         Args.with("ids", new Integer[]{new Integer(1),
                                                                        new Integer(2)}));

        assertEquals(2, things.size());
    }

    public void _testInWithQuotedQuestionMark() throws Exception
    {
        handle.prepareBatch("insert into something (id, name) values (:id, :name)")
                .add(new Something(1, "o ? o"))
                .add(new Something(2, "2 ? 2"))
                .add(new Something(3, "three"))
                .add(new Something(4, "four"))
                .execute();

        final List things = handle.query("select * from something where id in (:ids) " +
                                         "",
                                         Args.with("ids", new Integer[]{new Integer(1),
                                                                        new Integer(2)}));

        assertEquals(2, things.size());
    }

}
