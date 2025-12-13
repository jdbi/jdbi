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

import com.google.common.collect.ImmutableSet;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.guava.GuavaPlugin;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.util.Arrays.asList;

import static org.assertj.core.api.Assertions.assertThat;

public class TestInClauseExpansion {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugins(new SqlObjectPlugin(), new GuavaPlugin())
            .withInitializer(TestingInitializers.something());

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
    }

    @Test
    public void testInClauseExpansion() {
        handle.execute("insert into something (name, id) values ('Brian', 1), ('Jeff', 2), ('Tom', 3)");

        DAO dao = handle.attach(DAO.class);

        assertThat(dao.findIdsForNames(asList(1, 2))).containsExactly("Brian", "Jeff");
    }

    public interface DAO {
        @SqlQuery("select name from something where id in (<names>)")
        ImmutableSet<String> findIdsForNames(@BindList List<Integer> names);
    }

}
