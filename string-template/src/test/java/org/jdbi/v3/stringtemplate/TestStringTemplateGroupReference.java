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

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.jdbi.v3.sqlobject.SqlUpdate;
import org.jdbi.v3.sqlobject.customizers.Define;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestStringTemplateGroupReference {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @Before
    public void setUp() throws Exception {
        handle = db.getSharedHandle();
    }

    @Test
    public void testCrossTemplateReference() {
        Dao dao = handle.attach(Dao.class);

        dao.insert(1, "Bob");
        dao.insert(2, "Alice");

        assertThat(dao.list(false, "")).containsExactly(1L, 2L);
        assertThat(dao.list(true, "name")).containsExactly(2L, 1L);
    }

    @UseStringTemplateSqlLocator
    public interface Dao {
        @SqlUpdate
        void insert(long id, String name);

        @SqlQuery
        List<Long> list(@Define("sort") boolean sort,
                        @Define("sortBy") String sortBy);
    }
}
