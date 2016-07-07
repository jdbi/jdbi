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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.SortedSet;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.guava.GuavaCollectors;
import org.jdbi.v3.sqlobject.customizers.RegisterCollectorFactory;
import org.junit.Rule;
import org.junit.Test;

public class TestCollectorFactory {

    @Rule
    public H2DatabaseRule h2 = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Test
    public void testExists() throws Exception {
        Handle h = h2.getSharedHandle();
        h.execute("insert into something (id, name) values (1, 'Coda')");

        Optional<String> rs = h.createQuery("select name from something where id = :id")
                .bind("id", 1)
                .mapTo(String.class)
                .collect(GuavaCollectors.toOptional());

        assertThat(rs.isPresent(), equalTo(true));
        assertThat(rs.get(), equalTo("Coda"));
    }

    @Test
    public void testDoesNotExist() throws Exception {
        Handle h = h2.getSharedHandle();
        h.execute("insert into something (id, name) values (1, 'Coda')");

        Optional<String> rs = h.createQuery("select name from something where id = :id")
                .bind("id", 2)
                .mapTo(String.class)
                .collect(GuavaCollectors.toOptional());

        assertThat(rs.isPresent(), equalTo(false));
    }

    @Test
    public void testOnList() throws Exception {
        Handle h = h2.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'Coda')");
        h.execute("insert into something (id, name) values (2, 'Brian')");

        ImmutableList<String> rs = h.createQuery("select name from something order by id")
                .mapTo(String.class)
                .collect(GuavaCollectors.toImmutableList());

        assertThat(rs, equalTo(ImmutableList.of("Coda", "Brian")));
    }

    @Test
    public void testWithSqlObject() throws Exception {
        Dao dao = h2.getJdbi().onDemand(Dao.class);
        dao.insert(new Something(1, "Coda"));
        dao.insert(new Something(2, "Brian"));

        ImmutableList<String> rs = dao.findAll();
        assertThat(rs, equalTo(ImmutableList.of("Coda", "Brian")));
    }

    @Test
    public void testWithSqlObjectSingleValue() throws Exception {
        Dao dao = h2.getJdbi().onDemand(Dao.class);
        dao.insert(new Something(1, "Coda"));
        dao.insert(new Something(2, "Brian"));

        Optional<String> rs = dao.findNameById(1);
        assertThat(rs, equalTo(Optional.of("Coda")));

        rs = dao.smartFindNameById(1);
        assertThat(rs, equalTo(Optional.of("Coda")));

        rs = dao.inheritedGenericFindNameById(1);
        assertThat(rs, equalTo(Optional.of("Coda")));
    }

    @Test
    public void testWithSqlObjectSetReturnValue() throws Exception {
        Dao dao = h2.getJdbi().onDemand(Dao.class);
        dao.insert(new Something(1, "Coda"));
        dao.insert(new Something(2, "Brian"));

        SortedSet<String> rs = dao.findAllAsSet();
        assertThat(rs, equalTo(ImmutableSortedSet.of("Brian", "Coda")));
    }

    @RegisterCollectorFactory(GuavaCollectors.Factory.class)
    public interface Dao extends Base<String> {
        @SqlQuery("select name from something order by id")
        ImmutableList<String> findAll();

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
