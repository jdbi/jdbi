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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestBindListParameter {

    private Jdbi db;
    private Handle handle;
    private MyDAO dao;

    @Before
    public void setUp() {
        db = Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID());
        db.installPlugin(new SqlObjectPlugin());
        handle = db.open();
        handle.createUpdate(
                "create table foo (id int, bar varchar(100) default null);")
                .execute();
        dao = db.onDemand(MyDAO.class);
    }

    @After
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
        dao.works(Lists.newArrayList(1L, 2L));
    }

    @Test
    public void testIds() {
        dao.ids(Lists.newArrayList(1, 2));
    }

    private interface MyDAO {
        @SqlQuery("select count(*) from foo where bar < 12 and id in (<ids>)")
        int broken();

        @SqlQuery("select count(*) from foo where bar \\< 12 and id in (<ids>)")
        int works(@BindList List<Long> ids);

        @SqlQuery("select count(*) from foo where id in (<ids>)")
        int ids(@BindList List<Integer> ids);
    }
}
