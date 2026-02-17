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
package org.jdbi.sqlobject;

import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit5.JdbiExtension;
import org.jdbi.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestGetGeneratedKeys {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    public interface DAO {

        @SqlUpdate("insert into something (name) values (:name)")
        @GetGeneratedKeys
        long insert(@Bind("name") String name);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") long id);

        @SqlUpdate("insert into something (name) values (:it)")
        @GetGeneratedKeys
        String generatedKeyReturnType(@Bind String name);
    }

    @Test
    public void testFoo() {
        h2Extension.getJdbi().useExtension(DAO.class, dao -> {
            long brianId = dao.insert("Brian");
            long keithId = dao.insert("Keith");

            assertThat(dao.findNameById(brianId)).isEqualTo("Brian");
            assertThat(dao.findNameById(keithId)).isEqualTo("Keith");
        });
    }
}
