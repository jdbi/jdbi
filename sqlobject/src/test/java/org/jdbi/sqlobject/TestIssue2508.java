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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

import org.jdbi.sqlobject.customizer.BindBean;
import org.jdbi.sqlobject.customizer.Define;
import org.jdbi.sqlobject.statement.SqlBatch;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestIssue2508 {
    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin()).withInitializer(TestingInitializers.something());

    @Test
    public void testIssue2508() {
        var data = new ArrayList<Something>();

        for (int i = 0; i < 10; i++) {
            h2Extension.getSharedHandle().execute("INSERT INTO something (id, name) VALUES (?, ?)", i, String.valueOf(i));

            Something something = new Something(i, UUID.randomUUID().toString());
            data.add(something);
        }

        h2Extension.getJdbi().useExtension(Dao.class, d -> d.updateAll("something", data));

        List<String> result = h2Extension.getJdbi().withExtension(Dao.class, d -> d.getNames("something"));

        List<String> names = data.stream()
            .map(Something::getName)
            .toList();

        assertThat(result).containsAll(names);
    }

    public interface Dao {
        @SqlBatch("update <table> set name = :name where id = :id")
        void updateAll(@Define("table") String tableName, @BindBean Iterable<Something> data);

        @SqlQuery("select name from <table> order by id")
        List<String> getNames(@Define("table") String table);
    }

    public static class Something {
        private final int id;
        private final String name;

        public Something(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Something.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("name='" + name + "'")
                .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Something something = (Something) o;
            return id == something.id && Objects.equals(name, something.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }

}
