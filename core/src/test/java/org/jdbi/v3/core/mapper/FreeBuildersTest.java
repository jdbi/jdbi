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
package org.jdbi.v3.core.mapper;

import org.inferred.freebuilder.FreeBuilder;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.freebuilder.JdbiFreeBuilders;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FreeBuildersTest {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule()
        .withConfig(JdbiFreeBuilders.class, c -> {
            return;
        });

    private Jdbi jdbi;
    private Handle h;

    @Before
    public void setup() {
        jdbi = dbRule.getJdbi();
        h = dbRule.getSharedHandle();
        h.execute("create table free_builders (t int, x varchar)");
    }

    // tag::example[]

    @FreeBuilder
    public interface FreeBuilderTrain {
        String name();
        int carriages();
        boolean observationCar();

        class Builder extends FreeBuildersTest_FreeBuilderTrain_Builder {}
    }

    @Test
    public void simpleTest() {
        jdbi.getConfig(JdbiFreeBuilders.class).registerFreeBuilder(FreeBuildersTest.FreeBuilderTrain.class);
        try (Handle handle = jdbi.open()) {
            handle.execute("create table train (name varchar, carriages int, observation_car boolean)");

            FreeBuilderTrain train = new FreeBuilderTrain.Builder()
                .name("Zephyr")
                .carriages(8)
                .observationCar(true)
                .build();

            assertThat(
                handle.createUpdate("insert into train(name, carriages, observation_car) values (:name, :carriages, :observationCar)")
                    .bindPojo(train)
                    .execute())
                .isEqualTo(1);

            assertThat(
                handle.createQuery("select * from train")
                    .mapTo(FreeBuildersTest.FreeBuilderTrain.class)
                    .one())
                .extracting("name", "carriages", "observationCar")
                .containsExactly("Zephyr", 8, true);
        }
    }
    // end::example[]

}
