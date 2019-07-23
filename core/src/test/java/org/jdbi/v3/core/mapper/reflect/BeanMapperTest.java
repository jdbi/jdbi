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
import org.jdbi.v3.core.Something;
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

public class BeanMapperTest {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withSomething();

    Handle handle;

    RowMapper<SampleBean> mapper = BeanMapper.of(SampleBean.class);

    @Before
    public void getHandle() throws SQLException {
        handle = dbRule.getSharedHandle();
    }

    public static class ColumnNameBean {
        int i;
        String s;

        @ColumnName("id")
        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }

        public String getS() {
            return s;
        }

        @ColumnName("name")
        public void setS(String s) {
            this.s = s;
        }
    }

    @Test
    public void testColumnNameAnnotation() {
        handle.registerRowMapper(BeanMapper.factory(ColumnNameBean.class));

        handle.execute("insert into something (id, name) values (1, 'foo')");

        ColumnNameBean bean = handle.createQuery("select * from something")
                .mapTo(ColumnNameBean.class)
                .one();

        assertThat(bean.getI()).isEqualTo(1);
        assertThat(bean.getS()).isEqualTo("foo");
    }

    @Test
    public void testNested() {
        handle.registerRowMapper(BeanMapper.factory(NestedBean.class));

        handle.execute("insert into something (id, name) values (1, 'foo')");

        assertThat(handle
            .createQuery("select id, name from something")
            .mapTo(NestedBean.class)
            .one())
            .extracting("nested.id", "nested.name")
            .containsExactly(1, "foo");
    }

    @Test
    public void testNestedStrict() {
        handle.getConfig(ReflectionMappers.class).setStrictMatching(true);
        handle.registerRowMapper(BeanMapper.factory(NestedBean.class));

        handle.execute("insert into something (id, name) values (1, 'foo')");

        assertThat(handle
            .createQuery("select id, name from something")
            .mapTo(NestedBean.class)
            .one())
            .extracting("nested.id", "nested.name")
            .containsExactly(1, "foo");

        assertThatThrownBy(() -> handle
            .createQuery("select id, name, 1 as other from something")
            .mapTo(NestedBean.class)
            .one())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("could not match properties for columns: [other]");
    }

    @Test
    public void testNestedNotReturned() {
        handle.registerRowMapper(BeanMapper.factory(NestedBean.class));
        assertThat(handle
            .createQuery("select 42 as testValue")
            .mapTo(NestedBean.class)
            .one())
            .extracting("testValue", "nested")
            .containsExactly(42, null);
    }

    public static class NestedBean {
        private Integer testValue;
        private Something nested;

        public Integer getTestValue() {
            return testValue;
        }

        public void setTestValue(Integer testValue) {
            this.testValue = testValue;
        }

        @Nested
        public Something getNested() {
            return nested;
        }

        public void setNested(Something nested) {
            this.nested = nested;
        }
    }

    @Test
    public void testNestedPrefix() {
        handle.registerRowMapper(BeanMapper.factory(NestedPrefixBean.class));

        handle.execute("insert into something (id, name) values (1, 'foo')");

        assertThat(handle
            .createQuery("select id nested_id, name nested_name from something")
            .mapTo(NestedPrefixBean.class)
            .one())
            .extracting("nested.id", "nested.name")
            .containsExactly(1, "foo");
    }

    @Test
    public void testNestedPrefixStrict() {
        handle.getConfig(ReflectionMappers.class).setStrictMatching(true);
        handle.registerRowMapper(BeanMapper.factory(NestedPrefixBean.class));

        handle.execute("insert into something (id, name, integerValue) values (1, 'foo', 5)"); // three, sir!

        assertThat(handle
            .createQuery("select id nested_id, name nested_name, integerValue from something")
            .mapTo(NestedPrefixBean.class)
            .one())
            .extracting("nested.id", "nested.name", "integerValue")
            .containsExactly(1, "foo", 5);

        assertThatThrownBy(() -> handle
            .createQuery("select id nested_id, name nested_name, 1 as other from something")
            .mapTo(NestedPrefixBean.class)
            .one())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("could not match properties for columns: [other]");

        assertThatThrownBy(() -> handle
            .createQuery("select id nested_id, name nested_name, 1 as nested_other from something")
            .mapTo(NestedPrefixBean.class)
            .one())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("could not match properties for columns: [nested_other]");
    }

    @Test
    public void testNestedPrefixNotReturned() {
        handle.registerRowMapper(BeanMapper.factory(NestedPrefixBean.class));
        assertThat(handle
            .createQuery("select 42 as integerValue")
            .mapTo(NestedPrefixBean.class)
            .one())
            .extracting("integerValue", "nested")
            .containsExactly(42, null);
    }

    public static class NestedPrefixBean {
        private Integer integerValue;
        private Something nested;

        public Integer getIntegerValue() {
            return integerValue;
        }

        public void setIntegerValue(Integer integerValue) {
            this.integerValue = integerValue;
        }

        public Something getNested() {
            return nested;
        }

        @Nested("nested")
        public void setNested(Something nested) {
            this.nested = nested;
        }
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
            assertThat(handle.select("select 42 as value, null as fk")
                    .map(BeanMapper.of(ClassPropagateNullThing.class))
                    .one())
                .isNull();
    }

    @Test
    public void classPropagateNotNull() {
            assertThat(handle.select("select 42 as value, 'a' as fk")
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

        @Nullable
        public void setIntegerValue(Integer integerValue) {
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
