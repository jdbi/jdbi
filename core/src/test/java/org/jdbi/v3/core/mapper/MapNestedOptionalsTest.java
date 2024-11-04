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

import jakarta.annotation.Nullable;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.DatabaseExtension;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class MapNestedOptionalsTest {

    public static final DatabaseExtension.DatabaseInitializer SOMETHING_INITIALIZER =
        h -> h.execute("create table something (id identity primary key, name varchar(50), intValue integer, nested_name varchar(50))");

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(SOMETHING_INITIALIZER);

    interface RowMapperFactory {
        <T> RowMapper<T> createRowMapper(Class<T> clazz);
    }

    private static Stream<Arguments> rowMappers() {
        RowMapperFactory constructorMapperFactory = ConstructorMapper::of;
        RowMapperFactory beanMapperFactory = BeanMapper::of;
        RowMapperFactory fieldMapperFactory = FieldMapper::of;


        return Stream.of(
            Arguments.argumentSet("ConstructorMapper", constructorMapperFactory),
            Arguments.argumentSet("BeanMapper", beanMapperFactory),
            Arguments.argumentSet("FieldMapper", fieldMapperFactory)
        );
    }

    @ParameterizedTest
    @MethodSource("rowMappers")
    void testMapNestedOptionalContainingOptionals(RowMapperFactory factory) {
        final Handle h = h2Extension.getSharedHandle();
        h.execute("insert into something(intValue, name) values(1, 'Duke')");
        h.execute("insert into something(intValue, name) values(null, null)");

        var resultList = (h.createQuery("select * from something order by id")
            .map(factory.createRowMapper(OptionalBeanWithNestedOptionals.class))
            .list());
        assertThat(resultList).hasSize(2);
        var first = resultList.get(0);
        var second = resultList.get(1);
        assertThat(first.bean).isPresent();
        assertThat(first.bean.get().intValue).isEqualTo(OptionalInt.of(1));
        assertThat(first.bean.get().name).isEqualTo(Optional.of("Duke"));
        // When all contained values are null then the Optional is empty
        assertThat(second.bean).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("rowMappers")
    void testMapNestedOptionalContainingRequiredPrimitiveField(RowMapperFactory factory) {
        final Handle h = h2Extension.getSharedHandle();
        h.execute("insert into something(intValue) values(1)");
        h.execute("insert into something(intValue) values(null)");

        var resultList = (h.createQuery("select * from something order by id")
            .map(factory.createRowMapper(OptionalBeanWithNestedWithRequiredPrimitiveField.class))
            .list());
        assertThat(resultList).hasSize(2);
        var first = resultList.get(0);
        assertThat(first.bean).isPresent();
        assertThat(first.bean.get().intValue).isEqualTo(1);
        // When all values are missing then the @Nested Optional is empty
        assertThat(resultList.get(1).bean).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("rowMappers")
    void testMapNestedOptionalContainingRequiredNonPrimitiveField(RowMapperFactory factory) {
        final Handle h = h2Extension.getSharedHandle();
        h.execute("insert into something(intValue, name) values(1, 'Duke')");
        h.execute("insert into something(intValue, name) values(null, null)");

        var resultList = (h.createQuery("select * from something order by id")
            .map(factory.createRowMapper(OptionalBeanWithNestedWithRequiredString.class))
            .list());
        assertThat(resultList).hasSize(2);
        var first = resultList.get(0);
        var second = resultList.get(1);
        assertThat(first.bean).isPresent();
        assertThat(first.bean.get().name).isEqualTo("Duke");
        // All null means it must be empty
        assertThat(second.bean).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("rowMappers")
    void testMapNestedOptionalWithPrefix(RowMapperFactory factory) {
        final Handle h = h2Extension.getSharedHandle();
        h.execute("insert into something(name) values('Duke')");
        h.execute("insert into something(name) values(null)");

        var resultList = (h.createQuery("select * from something order by id")
            .map(factory.createRowMapper(NullableNestedBean.class))
            .list());
        assertThat(resultList).hasSize(2);
        var first = resultList.get(0);
        var second = resultList.get(1);
        assertThat(first.bean).isNotNull();
        assertThat(first.bean.name).isEqualTo("Duke");
        // All null means it must be empty
        assertThat(second.bean).isNull();
    }


    public static class OptionalBean {

        public OptionalInt intValue;
        public Optional<String> name;

        @JdbiConstructor
        public OptionalBean(OptionalInt intValue, Optional<String> name) {
            this.intValue = intValue;
            this.name = name;
        }
        public OptionalBean() {
        }

        public OptionalInt getIntValue() {
            return intValue;
        }

        public void setIntValue(OptionalInt intValue) {
            this.intValue = intValue;
        }

        public Optional<String> getName() {
            return name;
        }

        public void setName(Optional<String> name) {
            this.name = name;
        }
    }

    public static class OptionalBeanWithNestedOptionals {

        @Nested
        public Optional<OptionalBean> bean;

        @JdbiConstructor
        public OptionalBeanWithNestedOptionals(@Nested Optional<OptionalBean> bean) {
            this.bean = bean;
        }
        public OptionalBeanWithNestedOptionals() {
        }

        public Optional<OptionalBean> getBean() {
            return bean;
        }

        @Nested
        public void setBean(Optional<OptionalBean> bean) {
            this.bean = bean;
        }
    }

    public static class NestedBeanWithPrimitive {
        public int intValue;

        @JdbiConstructor
        public NestedBeanWithPrimitive(int intValue) {
            this.intValue = intValue;
        }
        public NestedBeanWithPrimitive() {
        }

        public int getIntValue() {
            return intValue;
        }

        public void setIntValue(int intValue) {
            this.intValue = intValue;
        }
    }

    public static class OptionalBeanWithNestedWithRequiredPrimitiveField {

        @Nested
        public Optional<NestedBeanWithPrimitive> bean;

        @JdbiConstructor
        public OptionalBeanWithNestedWithRequiredPrimitiveField(@Nested Optional<NestedBeanWithPrimitive> bean) {
            this.bean = bean;
        }
        public OptionalBeanWithNestedWithRequiredPrimitiveField() {
        }

        public Optional<NestedBeanWithPrimitive> getBean() {
            return bean;
        }

        @Nested
        public void setBean(Optional<NestedBeanWithPrimitive> bean) {
            this.bean = bean;
        }
    }

    public static class NestedBeanWithString {
        public String name;

        @JdbiConstructor
        public NestedBeanWithString(String name) {
            this.name = name;
        }

        public NestedBeanWithString() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class OptionalBeanWithNestedWithRequiredString {

        @Nested
        public Optional<NestedBeanWithString> bean;

        @JdbiConstructor
        public OptionalBeanWithNestedWithRequiredString(@Nested Optional<NestedBeanWithString> bean) {
            this.bean = bean;
        }

        public OptionalBeanWithNestedWithRequiredString() {
        }

        public Optional<NestedBeanWithString> getBean() {
            return bean;
        }

        @Nested
        public void setBean(Optional<NestedBeanWithString> bean) {
            this.bean = bean;
        }
    }

    public static class NullableNestedBean {

        @Nullable @Nested
        public NestedBeanWithString bean;

        @JdbiConstructor
        public NullableNestedBean(@Nested @Nullable NestedBeanWithString bean) {
            this.bean = bean;
        }

        public NullableNestedBean() {
        }

        @Nullable
        public NestedBeanWithString getBean() {
            return bean;
        }

        @Nested
        public void setBean(@Nullable NestedBeanWithString bean) {
            this.bean = bean;
        }
    }
}
