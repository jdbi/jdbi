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
import org.jdbi.core.junit5.H2DatabaseExtension;
import org.jdbi.core.mapper.Nested;
import org.jdbi.core.mapper.PropagateNull;
import org.jdbi.core.mapper.reflect.ConstructorMapperTest.ClassPropagateNullThing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.core.junit5.H2DatabaseExtension.SOMETHING_INITIALIZER;

public class BeanMapperPropagateNullableTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(SOMETHING_INITIALIZER);

    private Handle handle;

    @BeforeEach
    public void setUp() {
        this.handle = h2Extension.getSharedHandle();
    }

    @Test
    public void propagateNull() {
        assertThat(handle
            .registerRowMapper(BeanMapper.factory(PropagateNullThing.class))
            .select("SELECT null as testValue, 'foo' as s")
            .mapTo(PropagateNullThing.class)
            .one())
            .isNull();
    }

    @Test
    public void propagateNotNull() {
        assertThat(handle
            .registerRowMapper(BeanMapper.factory(PropagateNullThing.class))
            .select("SELECT 42 as testValue, 'foo' as s")
            .mapTo(PropagateNullThing.class)
            .one())
            .extracting("testValue", "s")
            .containsExactly(42, "foo");
    }

    @Test
    public void nestedPropagateNull() {
        assertThat(handle
            .registerRowMapper(BeanMapper.factory(NestedPropagateNullThing.class))
            .select("SELECT 42 as integerValue, null as testValue, 'foo' as s")
            .mapTo(NestedPropagateNullThing.class)
            .one())
            .extracting("integerValue", "nested")
            .containsExactly(42, null);
    }

    @Test
    public void nestedPropagateNotNull() {
        assertThat(handle
            .registerRowMapper(BeanMapper.factory(NestedPropagateNullThing.class))
            .select("SELECT 42 as integerValue, 60 as testValue, 'foo' as s")
            .mapTo(NestedPropagateNullThing.class)
            .one())
            .extracting("integerValue", "nested.testValue", "nested.s")
            .containsExactly(42, 60, "foo");
    }

    @Test
    public void classPropagateNull() {
        assertThat(handle.select("select 42 as \"value\", null as fk")
            .map(BeanMapper.of(ClassPropagateNullThing.class))
            .one())
            .isNull();
    }

    @Test
    public void classPropagateNotNull() {
        assertThat(handle.select("select 42 as \"value\", 'a' as fk")
            .map(BeanMapper.of(ClassPropagateNullThing.class))
            .one())
            .extracting(cpnt -> cpnt.value)
            .isEqualTo(42);
    }

    public static class NestedPropagateNullThing {

        private Integer integerValue;
        private PropagateNullThing nested;

        public Integer getIntegerValue() {
            return integerValue;
        }

        public void setIntegerValue(@Nullable Integer integerValue) {
            this.integerValue = integerValue;
        }

        public PropagateNullThing getNested() {
            return nested;
        }

        @Nested
        public void setNested(PropagateNullThing nested) {
            this.nested = nested;
        }
    }

    public static class PropagateNullThing {

        private int testValue;
        private String s;

        public int getTestValue() {
            return testValue;
        }

        @PropagateNull
        public void setTestValue(int testValue) {
            this.testValue = testValue;
        }

        public String getS() {
            return s;
        }

        public void setS(String s) {
            this.s = s;
        }
    }
}
