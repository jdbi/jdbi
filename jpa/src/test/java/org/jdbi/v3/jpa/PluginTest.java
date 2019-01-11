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
package org.jdbi.v3.jpa;

import java.util.List;

import javax.persistence.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginTest {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugins();

    @Test
    public void testPluginInstallsJpaMapper() {
        Thing brian = new Thing(1, "Brian");
        Thing keith = new Thing(2, "Keith");

        ThingDao dao = dbRule.getSharedHandle().attach(ThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<Thing> rs = dao.list();

        assertThat(rs).containsOnlyOnce(brian, keith);
    }

    public interface ThingDao {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@BindJpa Thing thing);

        @SqlQuery("select id, name from something")
        List<Thing> list();
    }

    @Entity
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Thing {
        private int id;
        private String name;
    }
}
