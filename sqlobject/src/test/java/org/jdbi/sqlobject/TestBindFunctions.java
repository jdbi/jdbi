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

import org.jdbi.core.Handle;
import org.jdbi.sqlobject.customizer.BindMethods;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestBindFunctions {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    private Handle handle;

    private Dao dao;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();

        dao = handle.attach(Dao.class);
    }

    @Test
    public void testBindFunctions() {
        handle.execute("insert into something (id, name) values (1, 'Alice')");
        assertThat(dao.getName(1)).isEqualTo("Alice");

        dao.update(new FluentSomething(1, "Alicia"));
        assertThat(dao.getName(1)).isEqualTo("Alicia");
    }

    public static class FluentSomething {
        private final int id;
        private final String name;

        public FluentSomething(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int id() {
            return this.id;
        }

        public String name() {
            return this.name;
        }
    }

    public interface Dao {
        @SqlUpdate("update something set name=:name where id=:id")
        void update(@BindMethods FluentSomething thing);

        @SqlQuery("select name from something where id = :id")
        String getName(long id);
    }
}
