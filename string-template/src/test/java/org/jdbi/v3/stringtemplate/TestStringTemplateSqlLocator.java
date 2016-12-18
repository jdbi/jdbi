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
package org.jdbi.v3.stringtemplate;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestStringTemplateSqlLocator
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        handle = db.getSharedHandle();
    }

    @Test
    public void testBaz() throws Exception
    {
        Wombat wombat = handle.attach(Wombat.class);
        wombat.insert(new Something(7, "Henning"));

        String name = handle.createQuery("select name from something where id = 7")
                            .mapTo(String.class)
                            .findOnly();

        assertThat(name).isEqualTo("Henning");
    }

    @Test
    public void testBam() throws Exception
    {
        handle.execute("insert into something (id, name) values (6, 'Martin')");

        Something s = handle.attach(Wombat.class).findById(6L);
        assertThat(s.getName()).isEqualTo("Martin");
    }

    @Test
    public void testBap() throws Exception
    {
        handle.execute("insert into something (id, name) values (2, 'Bean')");
        Wombat w = handle.attach(Wombat.class);
        assertThat(w.findNameFor(2)).isEqualTo("Bean");
    }

    @Test
    public void testDefines() throws Exception
    {
        handle.attach(Wombat.class).weirdInsert("something", "id", "name", 5, "Bouncer");
        handle.attach(Wombat.class).weirdInsert("something", "id", "name", 6, "Bean");
        String name = handle.createQuery("select name from something where id = 5")
                            .mapTo(String.class)
                            .findOnly();

        assertThat(name).isEqualTo("Bouncer");
    }

    @Test
    public void testBatching() throws Exception
    {
        Wombat roo = handle.attach(Wombat.class);
        roo.insertBunches(new Something(1, "Jeff"), new Something(2, "Brian"));

        assertThat(roo.findById(1L)).isEqualTo(new Something(1, "Jeff"));
        assertThat(roo.findById(2L)).isEqualTo(new Something(2, "Brian"));
    }

    @UseStringTemplateSqlLocator
    @RegisterRowMapper(SomethingMapper.class)
    public interface Wombat
    {
        @SqlUpdate
        void insert(@BindBean Something s);

        @SqlQuery
        Something findById(@Bind("id") Long id);

        @SqlQuery
        String findNameFor(@Bind("id") int id);

        @SqlUpdate
        void weirdInsert(@Define("table") String table,
                         @Define("id_column") String idColumn,
                         @Define("value_column") String valueColumn,
                         @Bind("id") int id,
                         @Bind("value") String name);

        @SqlBatch
        void insertBunches(@BindBean Something... somethings);
    }

    public static class SomethingMapper implements RowMapper<Something>
    {
        @Override
        public Something map(ResultSet r, StatementContext ctx) throws SQLException
        {
            return new Something(r.getInt("id"), r.getString("name"));
        }
    }
}
