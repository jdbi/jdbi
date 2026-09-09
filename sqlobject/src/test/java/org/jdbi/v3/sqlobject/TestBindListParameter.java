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

import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.BindListStyle;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestBindListParameter {

    private Jdbi db;
    private Handle handle;
    private MyDAO dao;

    @BeforeEach
    public void setUp() {
        db = Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID());
        db.installPlugin(new SqlObjectPlugin());
        handle = db.open();
        handle.createUpdate(
                "create table foo (id int, bar varchar(100) default null);")
                .execute();
        dao = db.onDemand(MyDAO.class);
    }

    @AfterEach
    public void tearDown() {
        handle.execute("drop table foo");
        handle.close();
    }

    @Test
    public void testBrokenSyntax() {
        assertThatThrownBy(dao::broken).isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testWorks() {
        int result = dao.works(Lists.newArrayList(1L, 2L));
        assertThat(result).isZero();
    }

    @Test
    public void testIds() {
        int result = dao.ids(Lists.newArrayList(1, 2));
        assertThat(result).isZero();
    }

    @Test
    public void testRowsStyle() {
        handle.execute("insert into foo (id, bar) values (1, 'one'), (2, 'two'), (3, 'three')");

        assertThat(dao.barsForIds(Lists.newArrayList(3, 1))).containsExactly("one", "three");
    }

    @Test
    public void testMixedStylesOnOneMethod() {
        handle.execute("insert into foo (id, bar) values (1, 'one'), (2, 'two'), (3, 'three')");

        assertThat(dao.barsForIdsExcept(Lists.newArrayList(1, 2, 3), Lists.newArrayList(2))).containsExactly("one", "three");
    }

    @Test
    public void testRowsStyleOverridesHandleConfig() {
        db.getConfig(SqlStatements.class).setBindListStyle(BindListStyle.ROWS);
        handle.execute("insert into foo (id, bar) values (1, 'one'), (2, 'two')");

        assertThat(dao.ids(Lists.newArrayList(1, 2))).isEqualTo(2);
        assertThat(dao.barsForIds(Lists.newArrayList(2))).containsExactly("two");
    }

    private interface MyDAO {
        @SqlQuery("select count(*) from foo where bar < 12 and id in (<ids>)")
        int broken();

        @SqlQuery("select count(*) from foo where bar \\< 12 and id in (<ids>)")
        int works(@BindList List<Long> ids);

        @SqlQuery("select count(*) from foo where id in (<ids>)")
        int ids(@BindList List<Integer> ids);

        @SqlQuery("select f.bar from foo f join (values <ids>) as t(id) on f.id = t.id order by f.id")
        List<String> barsForIds(@BindList(style = BindListStyle.ROWS) List<Integer> ids);

        @SqlQuery("select f.bar from foo f join (values <ids>) as t(id) on f.id = t.id where f.id not in (<excluded>) order by f.id")
        List<String> barsForIdsExcept(@BindList(style = BindListStyle.ROWS) List<Integer> ids, @BindList List<Integer> excluded);
    }
}
