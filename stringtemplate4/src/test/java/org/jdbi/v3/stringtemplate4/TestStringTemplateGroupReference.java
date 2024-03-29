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

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestStringTemplateGroupReference {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
    }

    @Test
    public void testCrossTemplateReference() {
        Dao dao = handle.attach(Dao.class);

        dao.insert(1, "Bob");
        dao.insert(2, "Alice");

        assertThat(dao.list(false, "")).containsExactly(1L, 2L);
        assertThat(dao.list(true, "name")).containsExactly(2L, 1L);
    }

    @Test
    public void testImportReference() {
        Dao dao = handle.attach(Dao.class);

        dao.insert(1, "Bob");
        dao.insert(2, "Alice");

        assertThat(dao.listWithImport(false, "")).containsExactly(1L, 2L);
        assertThat(dao.listWithImport(true, "name")).containsExactly(2L, 1L);
    }

    @UseStringTemplateSqlLocator
    public interface Dao {
        @SqlUpdate
        void insert(long id, String name);

        @SqlQuery
        List<Long> list(@Define("sort") boolean sort,
                        @Define("sortBy") String sortBy);

        @SqlQuery
        List<Long> listWithImport(@Define("sort") boolean sort,
                                  @Define("sortBy") String sortBy);
    }
}
