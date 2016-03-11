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

import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.Handle;
import org.jdbi.v3.Something;
import org.jdbi.v3.sqlobject.customizers.Define;
import org.jdbi.v3.sqlobject.customizers.OverrideStatementLocatorWith;
import org.jdbi.v3.sqlobject.customizers.RegisterMapper;
import org.jdbi.v3.sqlobject.stringtemplate.StringTemplate3StatementLocator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestOverrideStatementLocatorWith
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    private Handle handle;

    @Before
    public void getHandle()
    {
        handle = db.getSharedHandle();
    }

    @Test
    public void testBaz() throws Exception
    {
        Kangaroo wombat = SqlObjects.attach(handle, Kangaroo.class);
        wombat.insert(new Something(7, "Henning"));

        String name = handle.createQuery("select name from something where id = 7")
                            .mapTo(String.class)
                            .findOnly();

        assertThat(name, equalTo("Henning"));
    }

    @Test
    public void testBam() throws Exception
    {
        handle.execute("insert into something (id, name) values (6, 'Martin')");

        Something s = SqlObjects.attach(handle, Kangaroo.class).findById(6L);
        assertThat(s.getName(), equalTo("Martin"));
    }

    @Test
    public void testBap() throws Exception
    {
        handle.execute("insert into something (id, name) values (2, 'Bean')");
        Kangaroo w = SqlObjects.attach(handle, Kangaroo.class);
        assertThat(w.findNameFor(2), equalTo("Bean"));
    }

    @Test
    public void testDefines() throws Exception
    {
        SqlObjects.attach(handle, Kangaroo.class).weirdInsert("something", "id", "name", 5, "Bouncer");
        String name = handle.createQuery("select name from something where id = 5")
                            .mapTo(String.class)
                            .findOnly();

        assertThat(name, equalTo("Bouncer"));
    }


    @OverrideStatementLocatorWith(StringTemplate3StatementLocator.class)
    @RegisterMapper(SomethingMapper.class)
    interface Kangaroo
    {
        @SqlUpdate
        void insert(@BindBean Something s);

        @SqlQuery
        Something findById(@Bind("id") Long id);

        @SqlQuery("select name from something where id = :id")
        String findNameFor(@Bind("id") int id);

        @SqlUpdate
        void weirdInsert(@Define("table") String table,
                         @Define("id_column") String idColumn,
                         @Define("value_column") String valueColumn,
                         @Bind("id") int id,
                         @Bind("value") String name);
    }

}
