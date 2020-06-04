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
        .withConfig(JdbiFreeBuilders.class, c -> c
            .registerFreeBuilder(
                Getter.class,
                IsIsIsIs.class)
        );

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
    public interface Train {
        String name();
        int carriages();
        boolean observationCar();

        class Builder extends FreeBuildersTest_Train_Builder {}
    }

    @Test
    public void simpleTest() {
        jdbi.getConfig(JdbiFreeBuilders.class).registerFreeBuilder(Train.class);
        try (Handle handle = jdbi.open()) {
            handle.execute("create table train (name varchar, carriages int, observation_car boolean)");

            Train train = new Train.Builder()
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
                    .mapTo(Train.class)
                    .one())
                .extracting("name", "carriages", "observationCar")
                .containsExactly("Zephyr", 8, true);
        }
    }
    // end::example[]

    @FreeBuilder
    public interface Getter {
        int getFoo();
        boolean isBar();

        class Builder extends FreeBuildersTest_Getter_Builder {}
    }

    @Test
    public void testGetterStyle() {
        final Getter expected = new Getter.Builder().setFoo(42).setBar(true).build();
        h.execute("create table getter(foo int, bar boolean)");
        assertThat(h.createUpdate("insert into getter(foo, bar) values (:foo, :bar)")
            .bindPojo(expected)
            .execute())
            .isEqualTo(1);
        assertThat(h.createQuery("select * from getter")
            .mapTo(Getter.class)
            .one())
            .isEqualTo(expected);
    }

    @FreeBuilder
    public interface IsIsIsIs {
        boolean is();
        boolean isFoo();
        String issueType();

        class Builder extends FreeBuildersTest_IsIsIsIs_Builder {}
    }

    @Test
    public void testIs() {
        IsIsIsIs value = new IsIsIsIs.Builder().is(true).isFoo(false).issueType("a").build();

        h.execute("create table isisisis (\"is\" boolean, foo boolean, issueType varchar)");
        h.createUpdate("insert into isisisis (\"is\", foo, issueType) values (:is, :foo, :issueType)")
            .bindPojo(value)
            .execute();
        assertThat(h.createQuery("select * from isisisis")
            .mapTo(IsIsIsIs.class)
            .one())
            .isEqualTo(value);
    }
}
