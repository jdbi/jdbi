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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.Nested;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jdbi.v3.core.junit5.H2DatabaseExtension.SOMETHING_INITIALIZER;

public class BeanMapperNestedTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(SOMETHING_INITIALIZER);

    private Handle handle;

    @BeforeEach
    public void setUp() {
        this.handle = h2Extension.getSharedHandle();
        handle.registerRowMapper(BeanMapper.factory(NestedBean.class));
        handle.registerRowMapper(BeanMapper.factory(NestedPrefixBean.class));
        handle.registerRowMapper(BeanMapper.factory(StrangePrefixBean.class));

        handle.execute("insert into something (id, name, integerValue) values (1, 'foo', 5)"); // three, sir!
    }

    @Test
    public void testNested() {
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
        assertThat(handle
            .createQuery("select 42 as testValue")
            .mapTo(NestedBean.class)
            .one())
            .extracting("testValue", "nested")
            .containsExactly(42, null);
    }

    @Test
    public void testNestedPrefix() {
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
        assertThat(handle
            .createQuery("select 42 as integerValue")
            .mapTo(NestedPrefixBean.class)
            .one())
            .extracting("integerValue", "nested")
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

        public Something getNested() {
            return nested;
        }

        @Nested
        public void setNested(Something nested) {
            this.nested = nested;
        }
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
    void testStrangePrefixes() {
        StrangePrefixBean bean = handle
            .createQuery("SELECT 'hello' AS \"a_bx\", 'world' AS \"ab_x\"")
            .mapTo(StrangePrefixBean.class)
            .one();

        assertThat(bean).isNotNull();
        assertThat(bean.getFirstBean()).isNotNull();
        assertThat(bean.getSecondBean()).isNotNull();

        assertThat(bean.getFirstBean().getValue()).isEqualTo("hello");
        assertThat(bean.getSecondBean().getValue()).isEqualTo("world");
    }

    public static class StrangePrefixBean {

        private FirstBean firstBean;
        private SecondBean secondBean;

        public FirstBean getFirstBean() {
            return firstBean;
        }

        @Nested("a")
        public void setFirstBean(FirstBean firstBean) {
            this.firstBean = firstBean;
        }

        public SecondBean getSecondBean() {
            return secondBean;
        }

        @Nested("ab")
        public void setSecondBean(SecondBean secondBean) {
            this.secondBean = secondBean;
        }
    }

    public static class FirstBean {
        private String value;

        public String getValue() {
            return value;
        }

        @ColumnName("bx")
        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class SecondBean {
        private String value;

        public String getValue() {
            return value;
        }

        @ColumnName("x")
        public void setValue(String value) {
            this.value = value;
        }
    }
}
