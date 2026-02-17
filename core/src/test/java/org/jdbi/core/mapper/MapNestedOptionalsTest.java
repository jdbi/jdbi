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
package org.jdbi.core.mapper;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

import org.jdbi.core.Handle;
import org.jdbi.core.junit5.DatabaseExtension;
import org.jdbi.core.junit5.H2DatabaseExtension;
import org.jdbi.core.mapper.reflect.BeanMapper;
import org.jdbi.core.mapper.reflect.ConstructorMapper;
import org.jdbi.core.mapper.reflect.FieldMapper;
import org.jdbi.core.mapper.reflect.JdbiConstructor;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

        var resultList = h.createQuery("select * from something order by id")
            .map(factory.createRowMapper(OptionalBeanWithNestedOptionals.class))
            .list();
        assertThat(resultList).hasSize(2);
        var first = resultList.get(0);
        var second = resultList.get(1);
        assertThat(first.bean).isPresent();
        assertThat(first.bean.get().intValue).isEqualTo(OptionalInt.of(1));
        assertThat(first.bean.get().name).isEqualTo(Optional.of("Duke"));
        assertThat(second.bean).isPresent();
        assertThat(second.bean.get().intValue).isEqualTo(OptionalInt.empty());
        assertThat(second.bean.get().name).isNotPresent();
    }

    @ParameterizedTest
    @MethodSource("rowMappers")
    void testMapNestedOptionalContainingRequiredPrimitiveField(RowMapperFactory factory) {
        final Handle h = h2Extension.getSharedHandle();
        h.execute("insert into something(intValue) values(1)");
        h.execute("insert into something(intValue) values(null)");

        var resultList = h.createQuery("select * from something order by id")
            .map(factory.createRowMapper(OptionalBeanWithNestedWithPropagateNullPrimitiveField.class))
            .list();
        assertThat(resultList).hasSize(2);
        var first = resultList.get(0);
        assertThat(first.bean).isPresent();
        assertThat(first.bean.get().intValue).isEqualTo(1);
        // When all values are missing then the @Nested Optional is empty
        assertThat(resultList.get(1).bean).isEmpty();
    }


    public static class OptionalBean {

        public OptionalInt intValue;
        public Optional<String> name;

        @JdbiConstructor
        public OptionalBean(OptionalInt intValue, Optional<String> name) {
            this.intValue = intValue;
            this.name = name;
        }
        public OptionalBean() {}

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
        public OptionalBeanWithNestedOptionals() {}

        public Optional<OptionalBean> getBean() {
            return bean;
        }

        @Nested
        public void setBean(Optional<OptionalBean> bean) {
            this.bean = bean;
        }
    }

    public static class NestedBeanWithPropagateNullPrimitive {
        @PropagateNull
        public int intValue;

        @JdbiConstructor
        public NestedBeanWithPropagateNullPrimitive(@PropagateNull int intValue) {
            this.intValue = intValue;
        }
        public NestedBeanWithPropagateNullPrimitive() {}

        public int getIntValue() {
            return intValue;
        }

        @PropagateNull
        public void setIntValue(int intValue) {
            this.intValue = intValue;
        }
    }

    public static class OptionalBeanWithNestedWithPropagateNullPrimitiveField {

        @Nested
        public Optional<NestedBeanWithPropagateNullPrimitive> bean;

        @JdbiConstructor
        public OptionalBeanWithNestedWithPropagateNullPrimitiveField(@Nested Optional<NestedBeanWithPropagateNullPrimitive> bean) {
            this.bean = bean;
        }
        public OptionalBeanWithNestedWithPropagateNullPrimitiveField() {}

        public Optional<NestedBeanWithPropagateNullPrimitive> getBean() {
            return bean;
        }

        @Nested
        public void setBean(Optional<NestedBeanWithPropagateNullPrimitive> bean) {
            this.bean = bean;
        }
    }
}
