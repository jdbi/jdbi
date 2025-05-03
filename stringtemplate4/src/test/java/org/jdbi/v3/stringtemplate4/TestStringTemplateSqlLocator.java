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
package org.jdbi.v3.stringtemplate4;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestStringTemplateSqlLocator {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    private Handle handle;
    private Wombat wombat;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
        wombat = handle.attach(Wombat.class);
    }

    @Test
    public void testBaz() {
        wombat.insert(new Something(7, "Henning"));

        String name = handle.createQuery("select name from something where id = 7")
                .mapTo(String.class)
                .one();

        assertThat(name).isEqualTo("Henning");
    }

    @Test
    public void testBam() {
        handle.execute("insert into something (id, name) values (6, 'Martin')");

        Something s = wombat.findById(6L);
        assertThat(s.getName()).isEqualTo("Martin");
    }

    @Test
    public void testBap() {
        handle.execute("insert into something (id, name) values (2, 'Bean')");
        assertThat(wombat.findNameFor(2)).isEqualTo("Bean");
    }

    @Test
    public void testDefines() {
        wombat.weirdInsert("something", "id", "name", 5, "Bouncer");
        wombat.weirdInsert("something", "id", "name", 6, "Bean");
        String name = handle.createQuery("select name from something where id = 5")
                .mapTo(String.class)
                .one();

        assertThat(name).isEqualTo("Bouncer");
    }

    @Test
    public void testConditionalExecutionWithNullValue() {
        wombat.insert(new Something(6, "Jack"));
        wombat.insert(new Something(7, "Wolf"));

        List<Something> somethings = wombat.findByIdOrUptoLimit(6, null);
        assertThat(somethings).hasSize(1);
    }

    @Test
    public void testConditionalExecutionWithNonNullValue() {
        wombat.insert(new Something(6, "Jack"));
        wombat.insert(new Something(7, "Wolf"));

        List<Something> somethings = wombat.findByIdOrUptoLimit(null, 8);
        assertThat(somethings).hasSize(2);
    }

    @Test
    public void testBatching() {
        wombat.insertBunches(new Something(1, "Jeff"), new Something(2, "Brian"));

        assertThat(wombat.findById(1L)).isEqualTo(new Something(1, "Jeff"));
        assertThat(wombat.findById(2L)).isEqualTo(new Something(2, "Brian"));
    }

    @UseStringTemplateSqlLocator
    @RegisterRowMapper(SomethingMapper.class)
    public interface Wombat {
        @SqlUpdate
        void insert(@BindBean Something s);

        @SqlQuery
        Something findById(@Bind("id") Long id);

        @SqlQuery
        List<Something> findByIdOrUptoLimit(@Bind("id") Integer id, @Define("idLimit") @Bind("idLimit") Integer idLimit);

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

    public static class SomethingMapper implements RowMapper<Something> {
        @Override
        public Something map(ResultSet r, StatementContext ctx) throws SQLException {
            return new Something(r.getInt("id"), r.getString("name"));
        }
    }
}
