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
import org.jdbi.sqlobject.customizer.BindFields;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestBindFields {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    private Dao dao;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();

        dao = handle.attach(Dao.class);

        handle.execute("CREATE TABLE the_table (id IDENTITY PRIMARY KEY, name varchar)");
    }

    public static class TestObject {
        public final int id;
        public final String name;

        public TestObject(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int id() {
            throw new RuntimeException("Should not be called.");
        }

        public int getId() {
            throw new RuntimeException("Should not be called.");
        }

        public int name() {
            throw new RuntimeException("Should not be called.");
        }

        public int getName() {
            throw new RuntimeException("Should not be called.");
        }
    }

    public interface Dao {
        @SqlUpdate("update the_table set name=:name where id=:id")
        void update(@BindFields TestObject thing);

        @SqlUpdate("update the_table set name=:thing.name where id=:thing.id")
        void updatePrefix(@BindFields("thing") TestObject thing);

        @SqlQuery("select name from the_table where id = :id")
        String getName(long id);
    }

    @Test
    public void testBindFields() {
        handle.execute("insert into the_table (id, name) values (1, 'Alice')");
        assertThat(dao.getName(1)).isEqualTo("Alice");

        dao.update(new TestObject(1, "Alicia"));
        assertThat(dao.getName(1)).isEqualTo("Alicia");
    }

    @Test
    public void testBindFieldsPrefix() {
        handle.execute("insert into the_table (id, name) values (2, 'Bob')");
        assertThat(dao.getName(2)).isEqualTo("Bob");

        dao.updatePrefix(new TestObject(2, "Rob"));
        assertThat(dao.getName(2)).isEqualTo("Rob");
    }
}
