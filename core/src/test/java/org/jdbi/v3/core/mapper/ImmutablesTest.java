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

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import org.assertj.core.api.Assertions;
import org.immutables.value.Value;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.annotation.Unmappable;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.immutables.JdbiImmutables;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ImmutablesTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance()
        .withConfig(JdbiImmutables.class, c -> c
            .registerImmutable(
                SubValue.class,
                FooBarBaz.class,
                Getter.class,
                ByteArray.class,
                DerivedProperty.class,
                Defaulty.class,
                IsIsIsIs.class,
                AlternateColumnName.class,
                GetterWithColumnName.class
            ).registerModifiable(FooBarBaz.class)
        );

    private Jdbi jdbi;
    private Handle h;

    @BeforeEach
    public void setup() {
        jdbi = h2Extension.getJdbi();
        h = h2Extension.getSharedHandle();
        h.execute("create table immutables (t int, x varchar)");

        Assertions.setExtractBareNamePropertyMethods(true);
    }

    // tag::example[]

    @Value.Immutable
    public interface Train {

        String name();

        int carriages();

        boolean observationCar();
    }

    @Test
    public void simpleTest() {
        jdbi.getConfig(JdbiImmutables.class).registerImmutable(Train.class);
        try (Handle handle = jdbi.open()) {
            handle.execute("create table train (name varchar, carriages int, observation_car boolean)");

            assertThat(
                handle.createUpdate("insert into train(name, carriages, observation_car) values (:name, :carriages, :observationCar)")
                    .bindPojo(ImmutableTrain.builder().name("Zephyr").carriages(8).observationCar(true).build())
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
    public void parameterizedTest() {
        assertThat(
            h.createUpdate("insert into immutables(t, x) values (:t, :x)")
                .bindPojo(ImmutableSubValue.<String, Integer>builder().t(42).x("foo").build())
                .execute())
            .isOne();

        assertThat(
            h.createQuery("select * from immutables")
                .mapTo(new GenericType<SubValue<String, Integer>>() {})
                .one())
            .extracting("t", "x")
            .containsExactly(42, "foo");
    }

    public interface BaseValue<T> {

        T t();
    }

    @Value.Immutable
    public interface SubValue<X, T> extends BaseValue<T> {

        X x();
    }

    @Value.Immutable
    @Value.Modifiable
    public interface FooBarBaz {

        int id();

        Optional<String> foo();

        OptionalInt bar();

        OptionalDouble baz();
    }

    @Test
    public void testModifiable() {
        h.execute("create table fbb (id int not null, foo varchar, bar int, baz real)");

        assertThat(h.createUpdate("insert into fbb (id, foo, bar, baz) values (:id, :foo, :bar, :baz)")
            .bindPojo(ModifiableFooBarBaz.create().setId(1).setFoo("foo").setBar(42).setBaz(1.0))
            .execute())
            .isOne();

        assertThat(h.createQuery("select * from fbb")
            .mapTo(ModifiableFooBarBaz.class)
            .one())
            .extracting("id", "foo", "bar", "baz")
            .containsExactly(1, Optional.of("foo"), OptionalInt.of(42), OptionalDouble.of(1.0));

        assertThat(h.createQuery("select * from fbb")
            .mapTo(ImmutableFooBarBaz.class)
            .one())
            .extracting("id", "foo", "bar", "baz")
            .containsExactly(1, Optional.of("foo"), OptionalInt.of(42), OptionalDouble.of(1.0));
    }

    @Value.Immutable
    @Value.Style(overshadowImplementation = true, get = {"is*", "get*"}, init = "set*")
    public interface Getter {

        int getFoo();

        boolean isBar();

        // Also test that we can also use overshadowed builders
        static Builder builder() {
            return new Builder();
        }

        class Builder extends ImmutableGetter.Builder {}
    }

    @Test
    public void testGetterStyle() {
        final Getter expected = Getter.builder().setFoo(42).setBar(true).build();
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

    @Value.Immutable
    public interface ByteArray {

        byte[] value();
    }

    @Test
    public void testByteArray() {
        final byte[] value = new byte[]{(byte) 42, (byte) 24};
        h.execute("create table bytearr(\"value\" bytea)");
        h.createUpdate("insert into bytearr(\"value\") values(:value)")
            .bindPojo(ImmutableByteArray.builder().value(value).build())
            .execute();
        assertThat(h.createQuery("select * from bytearr")
            .mapTo(ByteArray.class)
            .one()
            .value())
            .containsExactly(value);
    }

    @Value.Immutable
    public interface DerivedProperty {

        @Value.Default
        default int foo() {
            return 1;
        }

        @Value.Check
        default void checkFoo() {
            if (foo() == 999) {
                throw new Boom();
            }
        }

        @Value.Derived
        @Unmappable
        default int derivedFoo() {
            return foo() + 40;
        }
    }

    @Test
    public void testUnmappableProperties() {
        final DerivedProperty value = ImmutableDerivedProperty.builder().foo(2).build();
        h.execute("create table derived (foo int, derivedFoo int)");
        h.createUpdate("insert into derived(foo, derivedFoo) values (:foo, :derivedFoo)")
            .bindPojo(value)
            .execute();
        assertThat(h.createQuery("select * from derived")
            .mapTo(DerivedProperty.class)
            .one()
            .foo())
            .isEqualTo(value.foo());
    }

    @Test
    public void testCheckMethod() {
        assertThatThrownBy(() ->
            h.createQuery("select 999 as foo")
                .mapTo(DerivedProperty.class)
                .one())
            .isInstanceOf(Boom.class);
    }

    static class Boom extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    @Value.Immutable
    public interface IsIsIsIs {

        boolean is();

        boolean isFoo();

        String issueType();

        @ColumnName("isInactive")
            // skip automatic "is" removal
        boolean isInactive();
    }

    @Test
    public void testIs() {
        IsIsIsIs value = ImmutableIsIsIsIs.builder().is(true).isFoo(false).issueType("a").isInactive(true).build();
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
    public void testDefaultNotStoredInDb() {
        assertThat(h.createQuery("select null as defaulted")
            .mapTo(Defaulty.class)
            .one())
            .extracting(Defaulty::defaulted)
            .isEqualTo(42);
    }

    @Value.Immutable
    public interface Defaulty {

        @Value.Default
        default int defaulted() {
            return 42;
        }
    }

    @Test
    public void testAlternateColumnName() {
        assertThat(h.createQuery("select :TheAnswer as TheAnswer")
            .bindPojo(ImmutableAlternateColumnName.builder().answer(42).build())
            .mapTo(AlternateColumnName.class)
            .one())
            .extracting(AlternateColumnName::answer)
            .isEqualTo(42);
    }

    @Value.Immutable
    public interface AlternateColumnName {

        @ColumnName("TheAnswer")
        int answer();
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

    @Value.Immutable
    public interface GetterWithColumnName {

        @ColumnName("the_answer")
        int getAnswer();
    }
}
