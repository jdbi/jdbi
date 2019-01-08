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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.ImmutableTrain;
import org.jdbi.v3.core.mapper.ImmutablesTest.Train;
import org.jdbi.v3.core.mapper.immutables.JdbiImmutables;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.customizer.BindPojo;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestBindProperties {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule()
        .withPlugin(new SqlObjectPlugin())
        .withConfig(JdbiImmutables.class, c -> c.registerImmutable(Train.class));

    private Handle h;

    private Dao dao;

    @Before
    public void setUp() {
        h = dbRule.getSharedHandle();
        h.execute("create table train (name varchar, carriages int, observation_car boolean)");

        dao = h.attach(Dao.class);
    }

    @Test
    public void testBindBean() {
        assertThat(
            dao.insert(ImmutableTrain.builder()
                .name("Zephyr")
                .carriages(8)
                .observationCar(true)
                .build()))
            .isEqualTo(1);

        assertThat(dao.getTrains().get(0))
            .extracting("name", "carriages", "observationCar")
            .containsExactly("Zephyr", 8, true);
    }

    public interface Dao {
        @SqlUpdate("insert into train(name, carriages, observation_car) values (:name, :carriages, :observationCar)")
        int insert(@BindPojo Train train);

        @SqlQuery("select * from train")
        List<Train> getTrains();
    }
}
