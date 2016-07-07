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
import org.jdbi.v3.core.exception.UnableToCreateStatementException;
import org.jdbi.v3.unstable.BindIn;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

public class TestBindInParameter {

    private Jdbi dbi;
    private Handle handle;
    private MyDAO dao;

    @Before
    public void setUp() {
        dbi = Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID());
        dbi.installPlugin(new SqlObjectPlugin());
        handle = dbi.open();
        handle.createStatement(
                "create table foo (id int, bar varchar(100) default null);")
                .execute();
        dao = dbi.onDemand(MyDAO.class);
    }

    @After
    public void tearDown() throws Exception {
        handle.execute("drop table foo");
        handle.close();
    }

    @Test(expected = UnableToCreateStatementException.class)
    public void testBrokenSyntax() {
        dao.broken();
    }

    @Test
    public void testWorks() {
        dao.works(Lists.newArrayList(1L, 2L));
    }

    @Test
    public void testIds() {
        dao.ids(Lists.newArrayList(1, 2));
    }

    public interface MyDAO {
        @SqlQuery("select count(*) from foo where bar < 12 and id in (<ids>)")
        int broken();

        @SqlQuery("select count(*) from foo where bar \\< 12 and id in (<ids>)")
        int works(@BindIn("ids") List<Long> ids);

        @SqlQuery("select count(*) from foo where id in (<ids>)")
        int ids(@BindIn("ids") List<Integer> ids);
    }
}
