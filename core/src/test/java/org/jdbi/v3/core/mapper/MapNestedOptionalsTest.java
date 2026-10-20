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

import java.sql.Types;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

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

import static org.assertj.core.api.Assertions.assertThat;

public class MapNestedOptionalsTest {

    public static final DatabaseExtension.DatabaseInitializer SOMETHING_INITIALIZER =
        h -> h.execute("create table something (id identity primary key, name varchar(50), intValue integer, nested_name varchar(50))");

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(SOMETHING_INITIALIZER);

    interface RowMapperFactory {
        <T> RowMapper<T> createRowMapper(Class<T> clazz);
    }

    private static Stream<Arguments.ArgumentSet> rowMappers() {
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


    private record ParentVariant(String name, Class<? extends NestedOptionalHolder> type) {}

    private static Stream<Arguments> propagateNullParents() {
        var parents = List.of(
            new ParentVariant("class-level @PropagateNull", ParentOfClassPropagateNull.class),
            new ParentVariant("class-level @PropagateNull, @PropagateNull on parent", PropagateNullParentOfClassPropagateNull.class),
            new ParentVariant("attribute-level @PropagateNull", ParentOfAttributePropagateNull.class),
            new ParentVariant("attribute-level @PropagateNull, @PropagateNull on parent", PropagateNullParentOfAttributePropagateNull.class));

        return rowMappers().flatMap(mapper -> parents.stream().map(parent -> Arguments.argumentSet(
            mapper.getName() + ", " + parent.name(),
            mapper.get()[0],
            parent.type())));
    }

    @ParameterizedTest
    @MethodSource("propagateNullParents")
    void testNestedOptionalIsEmptyWhenPropagateNullColumnIsNull(RowMapperFactory factory, Class<? extends NestedOptionalHolder> type) {
        final Handle h = h2Extension.getSharedHandle();
        h.execute("insert into something(intValue, nested_name) values(1, 'Duke')");
        h.execute("insert into something(intValue, nested_name) values(null, 'Anonymous')");

        var resultList = h.createQuery("select intValue as nested_id, nested_name from something order by id")
            .map(factory.createRowMapper(type))
            .list();

        assertThat(resultList).hasSize(2);
        assertThat(resultList.get(0).getNested()).hasValueSatisfying(n -> {
            assertThat(n.getId()).isEqualTo(1);
            assertThat(n.getName()).isEqualTo("Duke");
        });
        assertThat(resultList.get(1)).isNotNull();
        assertThat(resultList.get(1).getNested()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("rowMappers")
    void testNestedOptionalInsideNestedOptionalWithPropagateNull(RowMapperFactory factory) {
        final Handle h = h2Extension.getSharedHandle();
        final RowMapper<OuterOfMiddle> mapper = factory.createRowMapper(OuterOfMiddle.class);
        final String sql = "select cast(:middleId as integer) as outer_id, cast(:innerId as integer) as outer_inner_id, 'Duke' as outer_inner_name";

        OuterOfMiddle bothPresent = h.createQuery(sql).bind("middleId", 1).bind("innerId", 2).map(mapper).one();
        assertThat(bothPresent.getOuter()).hasValueSatisfying(middle -> {
            assertThat(middle.getId()).isEqualTo(1);
            assertThat(middle.getInner()).hasValueSatisfying(inner -> {
                assertThat(inner.getId()).isEqualTo(2);
                assertThat(inner.getName()).isEqualTo("Duke");
            });
        });

        OuterOfMiddle innerNull = h.createQuery(sql).bind("middleId", 1).bindNull("innerId", Types.INTEGER).map(mapper).one();
        assertThat(innerNull.getOuter()).hasValueSatisfying(middle -> {
            assertThat(middle.getId()).isEqualTo(1);
            assertThat(middle.getInner()).isEmpty();
        });

        OuterOfMiddle middleNull = h.createQuery(sql).bindNull("middleId", Types.INTEGER).bind("innerId", 2).map(mapper).one();
        assertThat(middleNull).isNotNull();
        assertThat(middleNull.getOuter()).isEmpty();
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

    public interface IdAndName {
        Integer getId();

        String getName();
    }

    public interface NestedOptionalHolder {
        Optional<? extends IdAndName> getNested();
    }

    @PropagateNull("id")
    public static class ClassPropagateNullBean implements IdAndName {
        public Integer id;
        public String name;

        @JdbiConstructor
        public ClassPropagateNullBean(Integer id, String name) {
            this.id = id;
            this.name = name;
        }
        public ClassPropagateNullBean() {}

        @Override
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        @Override
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class AttributePropagateNullBean implements IdAndName {
        @PropagateNull
        public Integer id;
        public String name;

        @JdbiConstructor
        public AttributePropagateNullBean(@PropagateNull Integer id, String name) {
            this.id = id;
            this.name = name;
        }
        public AttributePropagateNullBean() {}

        @Override
        public Integer getId() {
            return id;
        }

        @PropagateNull
        public void setId(Integer id) {
            this.id = id;
        }

        @Override
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class ParentOfClassPropagateNull implements NestedOptionalHolder {
        @Nested("nested")
        public Optional<ClassPropagateNullBean> nested;

        @JdbiConstructor
        public ParentOfClassPropagateNull(@Nested("nested") Optional<ClassPropagateNullBean> nested) {
            this.nested = nested;
        }
        public ParentOfClassPropagateNull() {}

        @Override
        public Optional<ClassPropagateNullBean> getNested() {
            return nested;
        }

        @Nested("nested")
        public void setNested(Optional<ClassPropagateNullBean> nested) {
            this.nested = nested;
        }
    }

    public static class PropagateNullParentOfClassPropagateNull implements NestedOptionalHolder {
        @Nested("nested")
        @PropagateNull
        public Optional<ClassPropagateNullBean> nested;

        @JdbiConstructor
        public PropagateNullParentOfClassPropagateNull(@Nested("nested") @PropagateNull Optional<ClassPropagateNullBean> nested) {
            this.nested = nested;
        }
        public PropagateNullParentOfClassPropagateNull() {}

        @Override
        public Optional<ClassPropagateNullBean> getNested() {
            return nested;
        }

        @Nested("nested")
        @PropagateNull
        public void setNested(Optional<ClassPropagateNullBean> nested) {
            this.nested = nested;
        }
    }

    public static class ParentOfAttributePropagateNull implements NestedOptionalHolder {
        @Nested("nested")
        public Optional<AttributePropagateNullBean> nested;

        @JdbiConstructor
        public ParentOfAttributePropagateNull(@Nested("nested") Optional<AttributePropagateNullBean> nested) {
            this.nested = nested;
        }
        public ParentOfAttributePropagateNull() {}

        @Override
        public Optional<AttributePropagateNullBean> getNested() {
            return nested;
        }

        @Nested("nested")
        public void setNested(Optional<AttributePropagateNullBean> nested) {
            this.nested = nested;
        }
    }

    public static class PropagateNullParentOfAttributePropagateNull implements NestedOptionalHolder {
        @Nested("nested")
        @PropagateNull
        public Optional<AttributePropagateNullBean> nested;

        @JdbiConstructor
        public PropagateNullParentOfAttributePropagateNull(@Nested("nested") @PropagateNull Optional<AttributePropagateNullBean> nested) {
            this.nested = nested;
        }
        public PropagateNullParentOfAttributePropagateNull() {}

        @Override
        public Optional<AttributePropagateNullBean> getNested() {
            return nested;
        }

        @Nested("nested")
        @PropagateNull
        public void setNested(Optional<AttributePropagateNullBean> nested) {
            this.nested = nested;
        }
    }

    @PropagateNull("id")
    public static class MiddleWithNestedOptional {
        public Integer id;
        @Nested("inner")
        public Optional<ClassPropagateNullBean> inner;

        @JdbiConstructor
        public MiddleWithNestedOptional(Integer id, @Nested("inner") Optional<ClassPropagateNullBean> inner) {
            this.id = id;
            this.inner = inner;
        }

        public MiddleWithNestedOptional() {}

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Optional<ClassPropagateNullBean> getInner() {
            return inner;
        }

        @Nested("inner")
        public void setInner(Optional<ClassPropagateNullBean> inner) {
            this.inner = inner;
        }
    }

    public static class OuterOfMiddle {
        @Nested("outer")
        public Optional<MiddleWithNestedOptional> outer;

        @JdbiConstructor
        public OuterOfMiddle(@Nested("outer") Optional<MiddleWithNestedOptional> outer) {
            this.outer = outer;
        }

        public OuterOfMiddle() {}

        public Optional<MiddleWithNestedOptional> getOuter() {
            return outer;
        }

        @Nested("outer")
        public void setOuter(Optional<MiddleWithNestedOptional> outer) {
            this.outer = outer;
        }
    }
}
