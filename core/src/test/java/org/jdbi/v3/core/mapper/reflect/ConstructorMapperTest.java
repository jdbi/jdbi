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
package org.jdbi.v3.core.mapper.reflect;

import java.beans.ConstructorProperties;
import java.util.Optional;

import jakarta.annotation.Nullable;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.PropagateNull;
import org.jdbi.v3.core.statement.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

public class ConstructorMapperTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle()
            .registerRowMapper(ConstructorMapper.factory(ConstructorBean.class))
            .registerRowMapper(ConstructorMapper.factory(ConstructorPropertiesBean.class))
            .registerRowMapper(ConstructorMapper.factory(NamedParameterBean.class))
            .registerRowMapper(ConstructorMapper.factory(NullableNestedBean.class))
            .registerRowMapper(ConstructorMapper.factory(NullableParameterBean.class))
            .registerRowMapper(ConstructorMapper.factory(StaticFactoryMethodBean.class));

        handle.execute("CREATE TABLE bean (s varchar, i integer)");

        handle.execute("INSERT INTO bean VALUES('3', 2)");
    }

    private <T> T selectOne(String sql, Class<T> type) {
        try (Query query = handle.createQuery(sql)) {
            return query.mapTo(type).one();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "SELECT s, i FROM bean",
        "SELECT i, s FROM bean",
        "SELECT 1 as ignored, i, s FROM bean"
    })
    public void testParameterizedQueries(String query) {
        ConstructorBean bean = selectOne(query, ConstructorBean.class);

        assertThat(bean.s).isEqualTo("3");
        assertThat(bean.i).isEqualTo(2);
    }

    static class ConstructorBean {
        private final String s;
        private final int i;

        @SuppressWarnings("unused")
        ConstructorBean(int some, String other, long constructor) {
            throw new UnsupportedOperationException("You don't belong here!");
        }

        @JdbiConstructor
        ConstructorBean(String s, int i) {
            this.s = s;
            this.i = i;
        }
    }

    @Test
    public void testDuplicate() {
        assertThatThrownBy(() -> selectOne("SELECT i, s, s FROM bean", ConstructorBean.class))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testMismatch() {
        assertThatThrownBy(() -> selectOne("SELECT i, '7' FROM bean", ConstructorBean.class))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testNullableParameterPresent() {
        NullableParameterBean bean = selectOne("select s, i from bean", NullableParameterBean.class);
        assertThat(bean.s).isEqualTo("3");
        assertThat(bean.i).isEqualTo(2);
    }

    @Test
    public void testNullableParameterAbsent() {
        NullableParameterBean bean = selectOne("select i from bean", NullableParameterBean.class);
        assertThat(bean.s).isNull();
        assertThat(bean.i).isEqualTo(2);
    }

    @Test
    public void testNonNullableAbsent() {
        assertThatThrownBy(() -> selectOne("select s from bean", NullableParameterBean.class))
            .isInstanceOf(IllegalArgumentException.class);
    }

    static class NullableParameterBean {
        private final String s;
        private final int i;

        NullableParameterBean(@Nullable String s, int i) {
            this.s = s;
            this.i = i;
        }
    }

    @Test
    public void testName() {
        NamedParameterBean nb = selectOne("SELECT 3 AS xyz", NamedParameterBean.class);
        assertThat(nb.i).isEqualTo(3);
    }

    static class NamedParameterBean {
        final int i;
        NamedParameterBean(@ColumnName("xyz") int i) {
            this.i = i;
        }
    }

    @Test
    public void testConstructorProperties() {
        final ConstructorPropertiesBean cpi = handle
            .createQuery("SELECT * FROM bean")
            .mapTo(ConstructorPropertiesBean.class)
            .one();
        assertThat(cpi.s).isEqualTo("3");
        assertThat(cpi.i).isEqualTo(2);
    }

    static class ConstructorPropertiesBean {
        final String s;
        final int i;

        ConstructorPropertiesBean() {
            this.s = null;
            this.i = 0;
        }

        @ConstructorProperties({"s", "i"})
        ConstructorPropertiesBean(String x, int y) {
            this.s = x;
            this.i = y;
        }
    }

    @Test
    public void nestedParameters() {
        assertThat(handle
                .registerRowMapper(ConstructorMapper.factory(NestedBean.class))
                .select("select s, i from bean")
                .mapTo(NestedBean.class)
                .one())
                        .extracting("nested.s", "nested.i")
                        .containsExactly("3", 2);
    }

    @Test
    public void nestedParametersStrict() {
        handle.getConfig(ReflectionMappers.class).setStrictMatching(true);
        handle.registerRowMapper(ConstructorMapper.factory(NestedBean.class));

        assertThat(handle
                .registerRowMapper(ConstructorMapper.factory(NestedBean.class))
                .select("select s, i from bean")
                .mapTo(NestedBean.class)
                .one())
                        .extracting("nested.s", "nested.i")
                        .containsExactly("3", 2);

        assertThatThrownBy(() -> handle
                .createQuery("select s, i, 1 as other from bean")
                .mapTo(NestedBean.class)
                .one())
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("could not match parameters for columns: [other]");
    }

    static class NestedBean {
        final ConstructorBean nested;

        NestedBean(@Nested ConstructorBean nested) {
            this.nested = nested;
        }
    }

    @Test
    public void nullableNestedParameterPresent() {
        NullableNestedBean bean = selectOne("select 'a' a, s, i from bean", NullableNestedBean.class);
        assertThat(bean.a).isEqualTo("a");
        assertThat(bean.nested).isNotNull();
        assertThat(bean.nested.s).isEqualTo("3");
        assertThat(bean.nested.i).isEqualTo(2);
    }

    @Test
    public void nullableNestedNullableParameterAbsent() {
        NullableNestedBean bean = selectOne("select 'a' a, i from bean", NullableNestedBean.class);
        assertThat(bean.a).isEqualTo("a");
        assertThat(bean.nested).isNotNull();
        assertThat(bean.nested.s).isNull();
        assertThat(bean.nested.i).isEqualTo(2);
    }

    @Test
    public void allColumnsOfNullableNestedObjectAbsent() {
        NullableNestedBean bean = selectOne("select 'a' a from bean", NullableNestedBean.class);
        assertThat(bean.a).isEqualTo("a");
        assertThat(bean.nested).isNull();
    }

    @Test
    public void nonNullableColumnOfNestedObjectAbsent() {
        assertThatThrownBy(() -> selectOne("select 'a' a, s from bean", NullableNestedBean.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static class NullableNestedBean {
        private final String a;
        private final NullableParameterBean nested;

        NullableNestedBean(String a,
                @Nullable @Nested NullableParameterBean nested) {
            this.a = a;
            this.nested = nested;
        }
    }

    @Test
    public void nestedPrefixParameters() {
        NestedPrefixBean result = handle
                .registerRowMapper(ConstructorMapper.factory(NestedPrefixBean.class))
                .select("select i nested_i, s nested_s from bean")
                .mapTo(NestedPrefixBean.class)
                .one();
        assertThat(result.nested.s).isEqualTo("3");
        assertThat(result.nested.i).isEqualTo(2);
    }

    @Test
    public void nestedPrefixParametersStrict() {
        handle.getConfig(ReflectionMappers.class).setStrictMatching(true);
        handle.registerRowMapper(ConstructorMapper.factory(NestedPrefixBean.class));

        assertThat(handle
                .createQuery("select i nested_i, s nested_s from bean")
                .mapTo(NestedPrefixBean.class)
                .one())
                        .extracting("nested.s", "nested.i")
                        .containsExactly("3", 2);

        assertThatThrownBy(() -> handle
                .createQuery("select i nested_i, s nested_s, 1 as other from bean")
                .mapTo(NestedPrefixBean.class)
                .one())
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("could not match parameters for columns: [other]");

        assertThatThrownBy(() -> handle
                .createQuery("select i nested_i, s nested_s, 1 as nested_other from bean")
                .mapTo(NestedPrefixBean.class)
                .one())
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("could not match parameters for columns: [nested_other]");
    }

    static class NestedPrefixBean {
        final ConstructorBean nested;

        NestedPrefixBean(@Nested("nested") ConstructorBean nested) {
            this.nested = nested;
        }
    }

    @Test
    public void propagateNull() {
        assertThat(handle
                .registerRowMapper(ConstructorMapper.factory(PropagateNullThing.class))
                .select("SELECT null as testValue, 'foo' as s")
                .mapTo(PropagateNullThing.class)
                .one())
                        .isNull();
    }

    @Test
    public void propagateNotNull() {
        assertThat(handle
                .registerRowMapper(ConstructorMapper.factory(PropagateNullThing.class))
                .select("SELECT 42 as testValue, 'foo' as s")
                .mapTo(PropagateNullThing.class)
                .one())
                        .extracting("testValue", "s")
                        .containsExactly(42, "foo");
    }

    @Test
    public void nestedPropagateNull() {
        assertThat(handle
                .registerRowMapper(ConstructorMapper.factory(NestedPropagateNullThing.class))
                .select("SELECT 42 as integerValue, null as testValue, 'foo' as s")
                .mapTo(NestedPropagateNullThing.class)
                .one())
                        .extracting("integerValue", "nested")
                        .containsExactly(42, null);
    }

    @Test
    public void nestedPropagateNotNull() {
        assertThat(handle
                .registerRowMapper(ConstructorMapper.factory(NestedPropagateNullThing.class))
                .select("SELECT 42 as integerValue, 60 as testValue, 'foo' as s")
                .mapTo(NestedPropagateNullThing.class)
                .one())
                        .extracting("integerValue", "nested.testValue", "nested.s")
                        .containsExactly(42, 60, "foo");
    }

    @Test
    public void classPropagateNull() {
        assertThat(handle.select("select 42 as \"value\", null as fk")
                .map(ConstructorMapper.of(ClassPropagateNullThing.class))
                .one()).isNull();
    }

    @Test
    public void classPropagateNotNull() {
        assertThat(handle.select("select 42 as \"value\", 'a' as fk")
                .map(ConstructorMapper.of(ClassPropagateNullThing.class))
                .one())
                        .extracting(cpnt -> cpnt.value)
                        .isEqualTo(42);
    }

    static class NestedPropagateNullThing {
        private final Integer integerValue;
        private final PropagateNullThing nested;

        NestedPropagateNullThing(Integer integerValue, @Nested PropagateNullThing nested) {
            this.integerValue = integerValue;
            this.nested = nested;
        }

        @Override
        public String toString() {
            return "NestedPropagateNullThing [integerValue=" + integerValue + ", nested=" + nested + "]";
        }
    }

    static class PropagateNullThing {
        private final Integer testValue;
        private final String s;

        PropagateNullThing(@PropagateNull Integer testValue, @Nullable String s) {
            this.testValue = testValue;
            this.s = s;
        }

        @Override
        public String toString() {
            return "PropagateNullThing [testValue=" + testValue + ", s=" + s + "]";
        }
    }

    @PropagateNull("fk")
    public static class ClassPropagateNullThing {
        int value;

        public ClassPropagateNullThing() {
            this(-1);
        }

        @JdbiConstructor
        ClassPropagateNullThing(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    @Test
    public void factoryMethod() {
        StaticFactoryMethodBean bean = selectOne("SELECT s, i FROM bean", StaticFactoryMethodBean.class);

        assertThat(bean.s).isEqualTo("3");
        assertThat(bean.i).isEqualTo(2);
    }

    @Test
    public void factoryMethodReversed() {
        StaticFactoryMethodBean bean = selectOne("SELECT i, s FROM bean", StaticFactoryMethodBean.class);

        assertThat(bean.s).isEqualTo("3");
        assertThat(bean.i).isEqualTo(2);
    }

    static class StaticFactoryMethodBean {
        private final String s;
        private final int i;

        StaticFactoryMethodBean(String s, int i, int pass) {
            assertThat(pass).isEqualTo(42); // Make sure this constructor is not used directly
            this.s = s;
            this.i = i;
        }

        @JdbiConstructor
        static StaticFactoryMethodBean create(String s, int i) {
            return new StaticFactoryMethodBean(s, i, 42);
        }
    }

    @Test
    public void multipleFactoryMethods() {
        assertThatThrownBy(() -> ConstructorMapper.factory(MultipleStaticFactoryMethodsBean.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageMatching("class .* may have at most one constructor or static factory method annotated @JdbiConstructor");
    }

    @SuppressWarnings("unused")
    static class MultipleStaticFactoryMethodsBean {
        @JdbiConstructor
        static MultipleStaticFactoryMethodsBean one(String s) {
            return new MultipleStaticFactoryMethodsBean();
        }

        @JdbiConstructor
        static MultipleStaticFactoryMethodsBean two(String s) {
            return new MultipleStaticFactoryMethodsBean();
        }
    }

    // Note: the tests with ClassWithGeneric* are to simulate the behavior which triggers https://bugs.openjdk.org/browse/JDK-8320575
    // A record constructor block will lose it's generic type information.
    static class ClassWithGenericThing {
        Optional<String> s;
        Optional<Integer> i;

        ClassWithGenericThing(Optional s, Optional i) {
            this.s = (Optional<String>) s;
            this.i = (Optional<Integer>) i;
        }
    }

    @Test
    public void testClassWithGenerics() {
        assertThat(handle
            .registerRowMapper(ConstructorMapper.factory(ClassWithGenericThing.class))
            .select("select s, i from bean")
            .mapTo(ClassWithGenericThing.class)
            .one())
                    .extracting("s", "i")
                    .containsExactly(Optional.of("3"), Optional.of(2));
    }

    static class ClassWithGenericThingJdbiConstructor {
        Optional<String> s;
        Optional<Integer> i;


        @JdbiConstructor
        ClassWithGenericThingJdbiConstructor(Optional s, Optional i) {
            this.s = (Optional<String>) s;
            this.i = (Optional<Integer>) i;
        }

        ClassWithGenericThingJdbiConstructor(String s, int i) {
            this.s = Optional.ofNullable(s);
            this.i = Optional.of(i);
        }
    }

    @Test
    public void testClassWithGenericsJdbiConstructor() {
        assertThat(handle
            .registerRowMapper(ConstructorMapper.factory(ClassWithGenericThingJdbiConstructor.class))
            .select("select s, i from bean")
            .mapTo(ClassWithGenericThingJdbiConstructor.class)
            .one())
                    .extracting("s", "i")
                    .containsExactly(Optional.of("3"), Optional.of(2));
    }

    static class ClassWithGenericThingAlternateJdbiConstructor {
        Optional<String> s;
        Optional<Integer> i;


        ClassWithGenericThingAlternateJdbiConstructor(Optional s, Optional i) {
            this.s = (Optional<String>) s;
            this.i = (Optional<Integer>) i;
        }

        @JdbiConstructor
        ClassWithGenericThingAlternateJdbiConstructor(Optional<String> s) {
            this.s = s;
            this.i = s.map(Integer::valueOf);
        }
    }

    @Test
    public void testClassWithGenericsCanonicalConstructor() {
        assertThat(handle
            .registerRowMapper(ConstructorMapper.factory(ClassWithGenericThingAlternateJdbiConstructor.class))
            .select("select s, i from bean")
            .mapTo(ClassWithGenericThingAlternateJdbiConstructor.class)
            .one())
                    .extracting("s", "i")
                    .containsExactly(Optional.of("3"), Optional.of(3));
    }


    static class ClassWithJustOneString {
        String si;
        ClassWithJustOneString(String si) {
            this.si = si;
        }
    }

    static class OneFactoryMethod {
        @SuppressWarnings("unused")
        static ClassWithJustOneString createClassWithStaticMethodAsJdbiConstructor(String s, int i) {
            return new ClassWithJustOneString(s + i);
        }
    }

    static class MultipleFactoryMethodsWhereOneIsAnnotated {
        @SuppressWarnings("unused")
        @JdbiConstructor
        static ClassWithJustOneString createClassWithJustOneString(String s, int i) {
            return new ClassWithJustOneString(s + i);
        }

        @SuppressWarnings("unused")
        static ClassWithJustOneString otherFactoryMethodWithoutAnnotation(String s, int it) {
            fail("This method should never be called, "
                 + "but exists to show that @JdbiConstructor is required when there are multiple static factory methods");
            return null;
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {OneFactoryMethod.class, MultipleFactoryMethodsWhereOneIsAnnotated.class})
    public void testStaticMethodFactory(Class<?> factoryMethodClass) {
        assertThat(handle
            .registerRowMapper(ConstructorMapper.factory(ClassWithJustOneString.class, factoryMethodClass))
            .select("select s, i from bean")
            .mapTo(ClassWithJustOneString.class)
            .one())
            .extracting("si")
            .isEqualTo("32");
    }

    static class NoFactoryMethod {}
    static class TwoFactoryMethods {
        @SuppressWarnings("unused")
        static ClassWithJustOneString first(String s, int it) {
            fail("This method should never be called");
            return null;
        }
        @SuppressWarnings("unused")
        static ClassWithJustOneString second(String s, int it) {
            fail("This method should never be called");
            return null;
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {NoFactoryMethod.class, TwoFactoryMethods.class})
    public void testNoSingleStaticMethodFactoryFound(Class<?> factoryMethodClass) {
        assertThatThrownBy(() -> ConstructorMapper.factory(ClassWithJustOneString.class, factoryMethodClass))
            .hasMessageContaining("%s must have exactly one factory method returning %s", factoryMethodClass, ClassWithJustOneString.class)
            .hasMessageContaining("or specify it with @JdbiConstructor");
    }

}
