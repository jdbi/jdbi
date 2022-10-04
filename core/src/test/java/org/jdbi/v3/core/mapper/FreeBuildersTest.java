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
import org.jdbi.v3.core.annotation.JdbiProperty;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.freebuilder.JdbiFreeBuilders;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class FreeBuildersTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance()
        .withConfig(JdbiFreeBuilders.class, c -> c
            .registerFreeBuilder(
                ByteArray.class,
                FreeBuilderClass.class,
                Getter.class,
                GetterWithColumnName.class,
                IsIsIsIs.class,
                SubValue.class,
                Train.class,
                UnmappableValue.class,
                UnnestedFreeBuilder.class)
        );

    private Jdbi jdbi;
    private Handle h;

    @BeforeEach
    public void setup() {
        jdbi = h2Extension.getJdbi();
        h = h2Extension.getSharedHandle();
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
                .isOne();

            assertThat(
                handle.createQuery("select * from train")
                    .mapTo(Train.class)
                    .one())
                .extracting("name", "carriages", "observationCar")
                .containsExactly("Zephyr", 8, true);
        }
    }

    // end::example[]

    @Test
    public void testUnnestedFreeBuilder() {
        final UnnestedFreeBuilder unnestedFreeBuilder = new UnnestedFreeBuilder.Builder().setTest("foo").build();
        h.execute("create table unnested_free_builders(test varchar)");

        assertThat(
            h.createUpdate("insert into unnested_free_builders(test) values (:test)")
            .bindPojo(unnestedFreeBuilder)
            .execute())
        .isOne();

        assertThat(
            h.createQuery("select * from unnested_free_builders")
            .mapTo(UnnestedFreeBuilder.class)
            .one())
        .isEqualTo(unnestedFreeBuilder);
    }

    public interface BaseValue<T> {
        T t();
    }

    @FreeBuilder
    public interface SubValue<X, T> extends BaseValue<T> {
        X x();

        class Builder<X, T> extends FreeBuildersTest_SubValue_Builder<X, T> {}
    }

    @Test
    public void testParameterizedBuilder() {
        assertThat(
            h.createUpdate("insert into free_builders(t, x) values (:t, :x)")
                .bindPojo(new SubValue.Builder<String, Integer>().t(42).x("foo").build())
                .execute())
            .isOne();

        assertThat(
            h.createQuery("select * from free_builders")
                .mapTo(new GenericType<SubValue<String, Integer>>() {})
                .one())
            .extracting("t", "x")
            .containsExactly(42, "foo");
    }

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
            .isOne();
        assertThat(h.createQuery("select * from getter")
            .mapTo(Getter.class)
            .one())
            .isEqualTo(expected);
    }

    @FreeBuilder
    public interface ByteArray {
        byte[] value();

        class Builder extends FreeBuildersTest_ByteArray_Builder {}
    }

    @Test
    public void testByteArray() {
        final byte[] value = new byte[] {(byte) 42, (byte) 24};
        h.execute("create table bytearr(\"value\" bytea)");
        h.createUpdate("insert into bytearr(\"value\") values(:value)")
            .bindPojo(new ByteArray.Builder().value(value).build())
            .execute();
        assertThat(h.createQuery("select * from bytearr")
            .mapTo(ByteArray.class)
            .one()
            .value())
            .containsExactly(value);
    }

    @FreeBuilder
    public interface IsIsIsIs {
        boolean is();
        boolean isFoo();
        String issueType();
        @ColumnName("isInactive")
        boolean isInactive();

        class Builder extends FreeBuildersTest_IsIsIsIs_Builder {}
    }

    @Test
    public void testIs() {
        IsIsIsIs value = new IsIsIsIs.Builder().is(true).isFoo(false).issueType("a").isInactive(true).build();

        h.execute("create table isisisis (\"is\" boolean, foo boolean, issueType varchar, IsInactive boolean)");
        h.createUpdate("insert into isisisis (\"is\", foo, issueType, isInactive) values (:is, :foo, :issueType, :isInactive)")
            .bindPojo(value)
            .execute();
        assertThat(h.createQuery("select * from isisisis")
            .mapTo(IsIsIsIs.class)
            .one())
            .isEqualTo(value);
    }

    @Test
    public void testGetterWithColumnName() {
        assertThat(h.createQuery("select :answer as the_answer")
            .bindBean(ImmutableGetterWithColumnName.builder().answer(42).build())
            .mapTo(GetterWithColumnName.class)
            .one())
            .extracting(GetterWithColumnName::getAnswer)
            .isEqualTo(42);
    }

    @FreeBuilder
    public interface GetterWithColumnName {
        @ColumnName("the_answer")
        int getAnswer();

        class Builder extends FreeBuildersTest_GetterWithColumnName_Builder {}
    }

    @FreeBuilder
    public abstract static class FreeBuilderClass {
        public abstract String getName();

        public static class Builder extends FreeBuildersTest_FreeBuilderClass_Builder {}
    }
    @Test
    public void testAbstractClass() {
        FreeBuilderClass value = new FreeBuilderClass.Builder().setName("name").build();
        h.execute("create table classes (name varchar)");
        h.createUpdate("insert into classes (name) values (:name)")
            .bindPojo(value)
            .execute();
        assertThat(h.createQuery("select * from classes")
            .mapTo(FreeBuilderClass.class)
            .one())
            .isEqualTo(value);
    }

    @FreeBuilder
    public interface UnmappableValue {
        int getFoo();

        @JdbiProperty(map = false)
        default int derivedFoo() {
            return 1;
        }

        class Builder extends FreeBuildersTest_UnmappableValue_Builder {}
    }

    @Test
    public void testUnmappableProperties() {
        final UnmappableValue value = new UnmappableValue.Builder().setFoo(4).build();
        h.execute("create table derived (foo int, derivedFoo int)");
        h.createUpdate("insert into derived(foo, derivedFoo) values (:foo, :derivedFoo)")
            .bindPojo(value)
            .execute();
        assertThat(h.createQuery("select * from derived")
            .mapTo(UnmappableValue.class)
            .one()
            .getFoo())
            .isEqualTo(value.getFoo());

    }

}
