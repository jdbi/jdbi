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
package org.jdbi.stringtemplate4;

import java.util.List;

import org.jdbi.core.Handle;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.customizer.Define;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit5.JdbiExtension;
import org.jdbi.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestConditionalStringTemplateLocator {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    @BeforeEach
    public void setUp() {
        Handle handle = h2Extension.getSharedHandle();
        handle.execute("insert into something (id, name) values (1, 'Martin')");
        handle.execute("insert into something (id, name) values (3, 'David')");
        handle.execute("insert into something (id, name) values (2, 'Joe')");
    }

    @Test
    public void testLocateFindAndSortByName() {
        List<Integer> ids = h2Extension.getSharedHandle().attach(Dao.class).findLocated(true, "name");
        assertThat(ids).containsExactly(3, 2, 1);
    }

    @Test
    public void testLocateFindWithoutSorting() {
        List<Integer> ids = h2Extension.getSharedHandle().attach(Dao.class).findLocated(false, "");
        assertThat(ids).containsExactly(1, 2, 3);
    }

    @Test
    public void testLocatedWithDifferentNameFindAndSortByName() {
        List<Integer> ids = h2Extension.getSharedHandle().attach(Dao.class).findLocatedWithDifferentName(true, "name");
        assertThat(ids).containsExactly(3, 2, 1);
    }

    @Test
    public void testLocatedWithDifferentNameFindWithoutSorting() {
        List<Integer> ids = h2Extension.getSharedHandle().attach(Dao.class).findLocatedWithDifferentName(false, "");
        assertThat(ids).containsExactly(1, 2, 3);
    }

    @Test
    public void testInlineFindAndSortByName() {
        List<Integer> ids = h2Extension.getSharedHandle().attach(Dao.class).findInline(true, "name");
        assertThat(ids).containsExactly(3, 2, 1);
    }

    @Test
    public void testInlineFindWithoutSorting() {
        List<Integer> ids = h2Extension.getSharedHandle().attach(Dao.class).findInline(false, "");
        assertThat(ids).containsExactly(1, 2, 3);
    }

    public interface Dao {
        @SqlQuery
        @UseStringTemplateSqlLocator
        List<Integer> findLocated(@Define("sort") boolean sort, @Define("sortBy") String sortBy);

        @SqlQuery("findLocated")
        @UseStringTemplateSqlLocator
        List<Integer> findLocatedWithDifferentName(@Define("sort") boolean sort, @Define("sortBy") String sortBy);

        @SqlQuery("select id from something order by <if(sort)> <sortBy>, <endif> id")
        @UseStringTemplateEngine
        List<Integer> findInline(@Define("sort") boolean sort, @Define("sortBy") String sortBy);
    }
}
