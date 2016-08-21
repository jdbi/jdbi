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

import java.util.List;

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.jdbi.v3.sqlobject.customizers.Define;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestConditionalStringTemplateLocator {

    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Before
    public void setUp() throws Exception {
        Handle handle = db.getSharedHandle();
        handle.insert("insert into something (id, name) values (1, 'Martin')");
        handle.insert("insert into something (id, name) values (3, 'David')");
        handle.insert("insert into something (id, name) values (2, 'Joe')");
    }

    @Test
    public void testLocateFindAndSortByName() {
        List<Integer> ids = db.getSharedHandle().attach(Dao.class).findLocated(true, "name");
        assertThat(ids).containsExactly(3, 2, 1);
    }

    @Test
    public void testLocateFindWithoutSorting() {
        List<Integer> ids = db.getSharedHandle().attach(Dao.class).findLocated(false, "");
        assertThat(ids).containsExactly(1, 2, 3);
    }

    @Test
    public void testInlineFindAndSortByName() {
        List<Integer> ids = db.getSharedHandle().attach(Dao.class).findInline(true, "name");
        assertThat(ids).containsExactly(3, 2, 1);
    }

    @Test
    public void testInlineFindWithoutSorting() {
        List<Integer> ids = db.getSharedHandle().attach(Dao.class).findInline(false, "");
        assertThat(ids).containsExactly(1, 2, 3);
    }

    public interface Dao {
        @SqlQuery
        @UseStringTemplateSqlLocator
        List<Integer> findLocated(@Define("sort") boolean sort, @Define("sortBy") String sortBy);

        @SqlQuery("select id from something order by <if(sort)> <sortBy>, <endif> id")
        @UseStringTemplateStatementRewriter
        List<Integer> findInline(@Define("sort") boolean sort, @Define("sortBy") String sortBy);
    }
}