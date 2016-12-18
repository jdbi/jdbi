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

import static org.assertj.core.api.Assertions.assertThat;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestBindBean {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    private Dao dao;

    @Before
    public void setUp() {
        handle = db.getSharedHandle();

        dao = handle.attach(Dao.class);
    }

    @Test
    public void testBindBean() {
        handle.insert("insert into something (id, name) values (1, 'Alice')");
        assertThat(dao.getName(1)).isEqualTo("Alice");

        dao.update(new Something(1, "Alicia"));
        assertThat(dao.getName(1)).isEqualTo("Alicia");
    }

    @Test
    public void testBindBeanPrefix() {
        handle.insert("insert into something (id, name) values (2, 'Bob')");
        assertThat(dao.getName(2)).isEqualTo("Bob");

        dao.updatePrefix(new Something(2, "Rob"));
        assertThat(dao.getName(2)).isEqualTo("Rob");
    }

    public interface Dao {
        @SqlUpdate("update something set name=:name where id=:id")
        void update(@BindBean Something thing);

        @SqlUpdate("update something set name=:thing.name where id=:thing.id")
        void updatePrefix(@BindBean("thing") Something thing);

        @SqlQuery("select name from something where id = :id")
        String getName(long id);
    }
}
