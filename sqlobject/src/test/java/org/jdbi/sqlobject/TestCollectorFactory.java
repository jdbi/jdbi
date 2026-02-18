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

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.jdbi.core.Handle;
import org.jdbi.core.Something;
import org.jdbi.guava.GuavaPlugin;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.customizer.BindBean;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestCollectorFactory {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin())
        .withPlugin(new GuavaPlugin());

    @Test
    public void testExists() {
        Handle h = h2Extension.getSharedHandle();
        h.execute("insert into something (id, name) values (1, 'Coda')");

        Optional<String> rs = h.createQuery("select name from something where id = :id")
            .bind("id", 1)
            .mapTo(String.class)
            .findFirst();

        assertThat(rs).contains("Coda");
    }

    @Test
    public void testDoesNotExist() {
        Handle h = h2Extension.getSharedHandle();
        h.execute("insert into something (id, name) values (1, 'Coda')");

        Optional<String> rs = h.createQuery("select name from something where id = :id")
                .bind("id", 2)
                .mapTo(String.class)
                .findFirst();

        assertThat(rs).isEmpty();
    }

    @Test
    public void testOnList() {
        Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'Coda')");
        h.execute("insert into something (id, name) values (2, 'Brian')");

        List<String> rs = h.createQuery("select name from something order by id")
                .mapTo(String.class)
                .collect(Collectors.toList());

        assertThat(rs).containsExactly("Coda", "Brian");
    }

    @Test
    public void testWithSqlObject() {
        Dao dao = h2Extension.getJdbi().onDemand(Dao.class);
        dao.insert(new Something(1, "Coda"));
        dao.insert(new Something(2, "Brian"));

        List<String> rs = dao.findAll();
        assertThat(rs).containsExactly("Coda", "Brian");
    }

    @Test
    public void testWithSqlObjectSingleValue() {
        Dao dao = h2Extension.getJdbi().onDemand(Dao.class);
        dao.insert(new Something(1, "Coda"));
        dao.insert(new Something(2, "Brian"));

        Optional<String> rs = dao.findNameById(1);
        assertThat(rs).contains("Coda");

        rs = dao.smartFindNameById(1);
        assertThat(rs).contains("Coda");

        rs = dao.inheritedGenericFindNameById(1);
        assertThat(rs).contains("Coda");
    }

    @Test
    public void testWithSqlObjectSetReturnValue() {
        Dao dao = h2Extension.getJdbi().onDemand(Dao.class);
        dao.insert(new Something(1, "Coda"));
        dao.insert(new Something(2, "Brian"));

        SortedSet<String> rs = dao.findAllAsSet();
        assertThat(rs).containsExactly("Brian", "Coda");
    }

    public interface Dao extends Base<String> {
        @SqlQuery("select name from something order by id")
        List<String> findAll();

        @SqlQuery("select name from something order by id")
        SortedSet<String> findAllAsSet();

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@BindBean Something it);

        @SqlQuery("select name from something where id = :id")
        Optional<String> findNameById(@Bind("id") int id);

        @SqlQuery("select name from something where id = :id")
        Optional<String> smartFindNameById(@Bind("id") int id);
    }

    public interface Base<T> {
        @SqlQuery("select name from something where id = :id")
        Optional<T> inheritedGenericFindNameById(@Bind("id") int id);
    }

}
