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
package org.jdbi.core.mapper.reflect;

import jakarta.annotation.Nullable;

import org.jdbi.core.Handle;
import org.jdbi.core.SampleBean;
import org.jdbi.core.internal.testing.H2DatabaseExtension;
import org.jdbi.core.mapper.Nested;
import org.jdbi.core.mapper.PropagateNull;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.mapper.reflect.ConstructorMapperTest.ClassPropagateNullThing;
import org.jdbi.core.statement.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FieldMapperTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(H2DatabaseExtension.SOMETHING_INITIALIZER);

    RowMapper<SampleBean> mapper = FieldMapper.of(SampleBean.class);

    static class ColumnNameThing {

        @ColumnName("id")
        int i;

        @ColumnName("name")
        String s;
    }

    @Test
    public void testColumnNameAnnotation() {
        Handle handle = h2Extension.getSharedHandle();

        handle.execute("insert into something (id, name) values (1, 'foo')");

        ColumnNameThing thing = handle.createQuery("select * from something")
            .map(FieldMapper.of(ColumnNameThing.class))
            .one();

        assertThat(thing.i).isOne();
        assertThat(thing.s).isEqualTo("foo");
    }

    @Test
    public void testNested() {
        Handle handle = h2Extension.getSharedHandle();

        handle.execute("insert into something (id, name) values (1, 'foo')");

        assertThat(handle
            .registerRowMapper(FieldMapper.factory(NestedThing.class))
            .select("SELECT id, name FROM something")
            .mapTo(NestedThing.class)
            .one())
            .extracting("nested.i", "nested.s")
            .containsExactly(1, "foo");
    }

    @Test
    public void testNestedStrict() {
        Handle handle = h2Extension.getSharedHandle();

        handle.getConfig(ReflectionMappers.class).setStrictMatching(true);
        handle.registerRowMapper(FieldMapper.factory(NestedThing.class));

        handle.execute("insert into something (id, name) values (1, 'foo')");

        assertThat(handle
            .registerRowMapper(FieldMapper.factory(NestedThing.class))
            .select("select id, name from something")
            .mapTo(NestedThing.class)
            .one())
            .extracting("nested.i", "nested.s")
            .containsExactly(1, "foo");

        assertThatThrownBy(() -> {
            try (Query query = handle.createQuery("select id, name, 1 as other from something")) {
                query.mapTo(NestedThing.class).one();
            }
        })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("could not match fields for columns: [other]");
    }

    static class NestedThing {

        @Nested
        ColumnNameThing nested;
    }

    @Test
    public void testNestedPresent() {
        Handle handle = h2Extension.getSharedHandle();

        assertThat(handle
            .registerRowMapper(FieldMapper.factory(NullableNestedThing.class))
            .select("SELECT 42 as testValue, 2 as i, '3' as s")
            .mapTo(NullableNestedThing.class)
            .one())
            .extracting("testValue", "nested.i", "nested.s")
            .containsExactly(42, 2, "3");
    }

    @Test
    public void testNestedHalfPresent() {
        Handle handle = h2Extension.getSharedHandle();

        assertThat(handle
            .registerRowMapper(FieldMapper.factory(NullableNestedThing.class))
            .select("SELECT 42 as testValue, '3' as s")
            .mapTo(NullableNestedThing.class)
            .one())
            .extracting("testValue", "nested.i", "nested.s")
            .containsExactly(42, null, "3");
    }

    @Test
    public void testNestedAbsent() {
        Handle handle = h2Extension.getSharedHandle();

        assertThat(handle
            .registerRowMapper(FieldMapper.factory(NullableNestedThing.class))
            .select("SELECT 42 as testValue")
            .mapTo(NullableNestedThing.class)
            .one())
            .extracting("testValue", "nested")
            .containsExactly(42, null);
    }

    @Test
    public void testNullableColumnAbsentButNestedPresent() {
        Handle handle = h2Extension.getSharedHandle();

        assertThat(handle
            .registerRowMapper(FieldMapper.factory(NullableNestedThing.class))
            .select("SELECT 's' s, 1 i")
            .mapTo(NullableNestedThing.class)
            .one())
            .extracting("testValue", "nested.s", "nested.i")
            .containsExactly(null, "s", 1);
    }

    @Test
    public void testNoRecognizedColumns() {
        Handle handle = h2Extension.getSharedHandle();
        handle.registerRowMapper(FieldMapper.factory(NullableNestedThing.class));

        assertThatThrownBy(() -> {
            try (Query query = handle.select("SELECT 'foo' bar")) {
                query.mapTo(NullableNestedThing.class).one();
            }
        })
            .isInstanceOf(IllegalArgumentException.class);
    }

    static class NullableNestedThing {

        @Nullable
        Integer testValue;

        @Nullable
        @Nested
        NullableThing nested;
    }

    static class NullableThing {

        @Nullable
        String s;

        @Nullable
        Integer i;
    }

    @Test
    public void testNestedPrefix() {
        Handle handle = h2Extension.getSharedHandle();

        handle.execute("insert into something (id, name) values (1, 'foo')");

        assertThat(handle
            .registerRowMapper(FieldMapper.factory(NestedPrefixThing.class))
            .select("select id nested_id, name nested_name from something")
            .mapTo(NestedPrefixThing.class)
            .one())
            .extracting("nested.i", "nested.s")
            .containsExactly(1, "foo");
    }

    @Test
    public void testNestedPrefixStrict() {
        Handle handle = h2Extension.getSharedHandle();

        handle.getConfig(ReflectionMappers.class).setStrictMatching(true);
        handle.registerRowMapper(FieldMapper.factory(NestedPrefixThing.class));

        handle.execute("insert into something (id, name, integerValue) values (1, 'foo', 5)"); // three, sir!

        assertThat(handle
            .createQuery("select id nested_id, name nested_name, integerValue from something")
            .mapTo(NestedPrefixThing.class)
            .one())
            .extracting("nested.i", "nested.s", "integerValue")
            .containsExactly(1, "foo", 5);

        assertThatThrownBy(() -> {
            try (Query query = handle.createQuery("select id nested_id, name nested_name, 1 as other from something")) {
                query.mapTo(NestedPrefixThing.class).one();
            }
        })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("could not match fields for columns: [other]");

        assertThatThrownBy(() -> {
            try (Query query = handle.createQuery("select id nested_id, name nested_name, 1 as nested_other from something")) {
                query.mapTo(NestedPrefixThing.class).one();
            }
        })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("could not match fields for columns: [nested_other]");
    }

    static class NestedPrefixThing {

        Integer integerValue;

        @Nested("nested")
        ColumnNameThing nested;
    }

    @Test
    public void propagateNull() {
        Handle handle = h2Extension.getSharedHandle();

        assertThat(handle
            .registerRowMapper(FieldMapper.factory(PropagateNullThing.class))
            .select("SELECT null as testValue, 'foo' as s")
            .mapTo(PropagateNullThing.class)
            .one())
            .isNull();
    }

    @Test
    public void propagateNotNull() {
        Handle handle = h2Extension.getSharedHandle();

        assertThat(handle
            .registerRowMapper(FieldMapper.factory(PropagateNullThing.class))
            .select("SELECT 42 as testValue, 'foo' as s")
            .mapTo(PropagateNullThing.class)
            .one())
            .extracting("testValue", "s")
            .containsExactly(42, "foo");
    }

    @Test
    public void nestedPropagateNull() {
        Handle handle = h2Extension.getSharedHandle();

        assertThat(handle
            .registerRowMapper(FieldMapper.factory(NestedPropagateNullThing.class))
            .select("SELECT 42 as integerValue, null as testValue, 'foo' as s")
            .mapTo(NestedPropagateNullThing.class)
            .one())
            .extracting("integerValue", "nested")
            .containsExactly(42, null);
    }

    @Test
    public void nestedPropagateNotNull() {
        Handle handle = h2Extension.getSharedHandle();

        assertThat(handle
            .registerRowMapper(FieldMapper.factory(NestedPropagateNullThing.class))
            .select("SELECT 42 as integerValue, 60 as testValue, 'foo' as s")
            .mapTo(NestedPropagateNullThing.class)
            .one())
            .extracting("integerValue", "nested.testValue", "nested.s")
            .containsExactly(42, 60, "foo");
    }

    @Test
    public void classPropagateNull() {
        Handle handle = h2Extension.getSharedHandle();

        assertThat(handle.select("select 42 as \"value\", null as fk")
            .map(FieldMapper.of(ClassPropagateNullThing.class))
            .one())
            .isNull();
    }

    @Test
    public void classPropagateNotNull() {
        Handle handle = h2Extension.getSharedHandle();

        assertThat(handle.select("select 42 as \"value\", 'a' as fk")
            .map(FieldMapper.of(ClassPropagateNullThing.class))
            .one())
            .extracting(cpnt -> cpnt.value)
            .isEqualTo(42);
    }

    static class NestedPropagateNullThing {

        Integer integerValue;

        @Nested
        PropagateNullThing nested;
    }

    static class PropagateNullThing {

        @PropagateNull
        int testValue;

        @Nullable
        String s;
    }

    @Test
    public void staticFieldIgnored() {
        Handle handle = h2Extension.getSharedHandle();

        assertThat(handle.select("select 42 as id")
                .map(FieldMapper.of(StaticIdThing.class))
                .one())
            .extracting(i -> i.id)
            .isEqualTo(42);
    }

    static class StaticIdThing {
        public static final String ID = "id";
        int id;
    }
}
