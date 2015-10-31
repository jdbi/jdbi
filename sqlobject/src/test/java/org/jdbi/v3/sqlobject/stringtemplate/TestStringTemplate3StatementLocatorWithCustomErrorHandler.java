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
package org.jdbi.v3.sqlobject.stringtemplate;

import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;

import org.antlr.stringtemplate.StringTemplateErrorListener;
import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.jdbi.v3.exceptions.UnableToCreateStatementException;
import org.jdbi.v3.sqlobject.SqlObjectBuilder;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.jdbi.v3.unstable.BindIn;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestStringTemplate3StatementLocatorWithCustomErrorHandler {

    private DBI dbi;
    private Handle handle;
    private MyDAO dao;

    @Before
    public void setUp() {
        dbi = DBI.create("jdbc:h2:mem:" + UUID.randomUUID());
        handle = dbi.open();
        handle.createStatement(
                "create table foo (id int, bar varchar(100) default null);")
                .execute();
        dao = SqlObjectBuilder.onDemand(dbi, MyDAO.class);
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

    @UseStringTemplate3StatementLocator(errorListener = MyTestCustomErrorHandler.class)
    public interface MyDAO {
        @SqlQuery("select * from foo where bar < 12 and id in (<ids>)")
        Object broken();

        @SqlQuery("select * from foo where bar \\< 12 and id in (<ids>)")
        Object works(@BindIn("ids") List<Long> ids);

        @SqlQuery("select * from foo where id in (<ids>)")
        Object ids(@BindIn("ids") List<Integer> ids);
    }

    public static class MyTestCustomErrorHandler implements StringTemplateErrorListener {

        @Override
        public void error(String msg, Throwable e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }

        @Override
        public void warning(String msg) {
            throw new RuntimeException("warning:" + msg);
        }
    }
}
