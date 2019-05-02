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

import java.sql.SQLException;

import javax.annotation.Nullable;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.SampleBean;
import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.PropagateNull;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapperTest.ClassPropagateNullThing;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FieldMapperTest {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withSomething();

    Handle handle;

    RowMapper<SampleBean> mapper = FieldMapper.of(SampleBean.class);

    @Before
    public void openHandle() throws SQLException {
        handle = dbRule.getSharedHandle();
    }

    static class ColumnNameThing {
        @ColumnName("id")
        int i;

        @ColumnName("name")
        String s;
    }

    @Test
    public void testColumnNameAnnotation() {
        handle.execute("insert into something (id, name) values (1, 'foo')");

        ColumnNameThing thing = sharedHandle.createQuery("select * from something")
                .map(FieldMapper.of(ColumnNameThing.class))
                .one();

        assertThat(thing.i).isEqualTo(1);
        assertThat(thing.s).isEqualTo("foo");
    }

    @Test
    public void testNested() {
        handle.execute("insert into something (id, name) values (1, 'foo')");

        assertThat(sharedHandle
            .registerRowMapper(FieldMapper.factory(NestedThing.class))
            .select("SELECT id, name FROM something")
            .mapTo(NestedThing.class)
            .one())
            .extracting("nested.i", "nested.s")
            .containsExactly(1, "foo");
    }

    @Test
    public void testNestedStrict() {
        handle.getConfig(ReflectionMappers.class).setStrictMatching(true);
        handle.registerRowMapper(FieldMapper.factory(NestedThing.class));

        sharedHandle.execute("insert into something (id, name) values (1, 'foo')");

        assertThat(sharedHandle
            .registerRowMapper(FieldMapper.factory(NestedThing.class))
            .select("select id, name from something")
            .mapTo(NestedThing.class)
            .one())
            .extracting("nested.i", "nested.s")
            .containsExactly(1, "foo");

        assertThatThrownBy(() -> sharedHandle
            .createQuery("select id, name, 1 as other from something")
            .mapTo(NestedThing.class)
            .one())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("could not match fields for columns: [other]");
    }

    static class NestedThing {
        @Nested
        ColumnNameThing nested;
    }

    @Test
    public void testNestedPresent() {
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
        assertThatThrownBy(() -> handle
            .registerRowMapper(FieldMapper.factory(NullableNestedThing.class))
            .select("SELECT 'foo' bar")
            .mapTo(NullableNestedThing.class)
            .one())
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
        handle.execute("insert into something (id, name) values (1, 'foo')");

        assertThat(sharedHandle
            .registerRowMapper(FieldMapper.factory(NestedPrefixThing.class))
            .select("select id nested_id, name nested_name from something")
            .mapTo(NestedPrefixThing.class)
            .one())
            .extracting("nested.i", "nested.s")
            .containsExactly(1, "foo");
    }

    @Test
    public void testNestedPrefixStrict() {
        handle.getConfig(ReflectionMappers.class).setStrictMatching(true);
        handle.registerRowMapper(FieldMapper.factory(NestedPrefixThing.class));

        sharedHandle.execute("insert into something (id, name, integerValue) values (1, 'foo', 5)"); // three, sir!

        assertThat(sharedHandle
            .createQuery("select id nested_id, name nested_name, integerValue from something")
            .mapTo(NestedPrefixThing.class)
            .one())
            .extracting("nested.i", "nested.s", "integerValue")
            .containsExactly(1, "foo", 5);

        assertThatThrownBy(() -> sharedHandle
            .createQuery("select id nested_id, name nested_name, 1 as other from something")
            .mapTo(NestedPrefixThing.class)
            .one())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("could not match fields for columns: [other]");

        assertThatThrownBy(() -> sharedHandle
            .createQuery("select id nested_id, name nested_name, 1 as nested_other from something")
            .mapTo(NestedPrefixThing.class)
            .one())
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
        assertThat(handle
            .registerRowMapper(FieldMapper.factory(PropagateNullThing.class))
            .select("SELECT null as testValue, 'foo' as s")
            .mapTo(PropagateNullThing.class)
            .one())
            .isNull();
    }

    @Test
    public void propagateNotNull() {
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
            assertThat(handle.select("select 42 as value, null as fk")
                    .map(FieldMapper.of(ClassPropagateNullThing.class))
                    .one())
                .isNull();
    }

    @Test
    public void classPropagateNotNull() {
            assertThat(handle.select("select 42 as value, 'a' as fk")
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
}
