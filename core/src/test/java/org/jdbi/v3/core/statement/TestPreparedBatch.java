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
package org.jdbi.v3.core.statement;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class TestPreparedBatch
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Test
    public void testBindBatch() throws Exception
    {
        Handle h = db.openHandle();
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        b.bind("id", 1).bind("name", "Eric").add();
        b.bind("id", 2).bind("name", "Brian").add();
        b.bind("id", 3).bind("name", "Keith").add();
        b.execute();

        List<Something> r = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(r).hasSize(3);
        assertThat(r.get(2).getName()).isEqualTo("Keith");
    }

    @Test
    public void testBigishBatch() throws Exception
    {
        Handle h = db.openHandle();
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        int count = 100;
        for (int i = 0; i < count; ++i)
        {
            b.bind("id", i).bind("name", "A Name").add();

        }
        b.execute();

        int row_count = h.createQuery("select count(id) from something").mapTo(int.class).findOnly();

        assertThat(row_count).isEqualTo(count);
    }

    @Test
    public void testBindProperties() throws Exception
    {
        Handle h = db.openHandle();
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (?, ?)");

        b.add(0, "Keith");
        b.add(1, "Eric");
        b.add(2, "Brian");

        b.execute();

        List<Something> r = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(r).hasSize(3);
        assertThat(r.get(2).getName()).isEqualTo("Brian");
    }

    @Test
    public void testBindMaps() throws Exception
    {
        Handle h = db.openHandle();
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        b.add(ImmutableMap.of("id", 0, "name", "Keith"));
        b.add(ImmutableMap.of("id", 1, "name", "Eric"));
        b.add(ImmutableMap.of("id", 2, "name", "Brian"));

        b.execute();

        List<Something> r = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(r).hasSize(3);
        assertThat(r.get(2).getName()).isEqualTo("Brian");
    }

    @Test
    public void testMixedModeBatch() throws Exception
    {
        Handle h = db.openHandle();
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        Map<String, Object> one = ImmutableMap.of("id", 0);
        b.bind("name", "Keith").add(one);
        b.execute();

        List<Something> r = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(r).extracting(Something::getName).containsExactly("Keith");
    }

    @Test
    public void testPositionalBinding() throws Exception
    {
        Handle h = db.openHandle();
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        b.bind(0, 0).bind(1, "Keith").add().execute();

        List<Something> r = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(r).extracting(Something::getName).containsExactly("Keith");
    }

    @Test
    public void testForgotFinalAdd() throws Exception
    {
        Handle h = db.openHandle();
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        b.bind("id", 1);
        b.bind("name", "Jeff");
        b.add();

        b.bind("id", 2);
        b.bind("name", "Tom");
        // forgot to add() here but we fix it up

        b.execute();

        assertThat(h.createQuery("select name from something order by id").mapTo(String.class).list())
                .containsExactly("Jeff", "Tom");
    }
}
