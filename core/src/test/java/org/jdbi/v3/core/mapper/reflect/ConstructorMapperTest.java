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
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConstructorMapperTest {

    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    @Before
    public void setUp() throws Exception {
        dbRule.getSharedHandle()
                .registerRowMapper(ConstructorMapper.factory(ConstructorBean.class))
                .registerRowMapper(ConstructorMapper.factory(NamedParameterBean.class))
                .registerRowMapper(ConstructorMapper.factory(ConstructorPropertiesBean.class));
        dbRule.getSharedHandle().execute("CREATE TABLE bean (s varchar, i integer)");

        dbRule.getSharedHandle().execute("INSERT INTO bean VALUES('3', 2)");
    }

    public ConstructorBean execute(String query) {
        return dbRule.getSharedHandle().createQuery(query).mapTo(ConstructorBean.class).findOnly();
    }

    @Test
    public void testSimple() throws Exception {
        ConstructorBean bean = execute("SELECT s, i FROM bean");

        assertThat(bean.s).isEqualTo("3");
        assertThat(bean.i).isEqualTo(2);
    }

    @Test
    public void testReversed() throws Exception {
        ConstructorBean bean = execute("SELECT i, s FROM bean");

        assertThat(bean.s).isEqualTo("3");
        assertThat(bean.i).isEqualTo(2);
    }

    @Test
    public void testExtra() throws Exception {
        ConstructorBean bean = execute("SELECT 1 as ignored, i, s FROM bean");

        assertThat(bean.s).isEqualTo("3");
        assertThat(bean.i).isEqualTo(2);
    }

    static class ConstructorBean {
        private final String s;
        private final int i;

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
    public void testDuplicate() throws Exception {
        assertThatThrownBy(() -> execute("SELECT i, s, s FROM bean")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testMismatch() throws Exception {
        assertThatThrownBy(() -> execute("SELECT i, '7' FROM bean")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testName() throws Exception {
        NamedParameterBean nb = dbRule.getSharedHandle().createQuery("SELECT 3 AS xyz")
                .mapTo(NamedParameterBean.class).findOnly();
        assertThat(nb.i).isEqualTo(3);
    }

    static class NamedParameterBean {
        final int i;
        NamedParameterBean(@ColumnName("xyz") int i) {
            this.i = i;
        }
    }

    @Test
    public void testConstructorProperties() throws Exception {
        final ConstructorPropertiesBean cpi = dbRule.getSharedHandle()
                .createQuery("SELECT * FROM bean")
                .mapTo(ConstructorPropertiesBean.class)
                .findOnly();
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
        assertThat(dbRule.getSharedHandle()
            .registerRowMapper(ConstructorMapper.factory(NestedBean.class))
            .select("select s, i from bean")
            .mapTo(NestedBean.class)
            .findOnly())
            .extracting("nested.s", "nested.i")
            .containsExactly("3", 2);
    }

    @Test
    public void nestedParametersStrict() {
        Handle handle = dbRule.getSharedHandle();
        handle.getConfig(ReflectionMappers.class).setStrictMatching(true);
        handle.registerRowMapper(ConstructorMapper.factory(NestedBean.class));

        assertThat(dbRule.getSharedHandle()
            .registerRowMapper(ConstructorMapper.factory(NestedBean.class))
            .select("select s, i from bean")
            .mapTo(NestedBean.class)
            .findOnly())
            .extracting("nested.s", "nested.i")
            .containsExactly("3", 2);

        assertThatThrownBy(() -> handle
            .createQuery("select s, i, 1 as other from bean")
            .mapTo(NestedBean.class)
            .findOnly())
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
    public void nestedPrefixParameters() {
        NestedPrefixBean result = dbRule.getSharedHandle()
            .registerRowMapper(ConstructorMapper.factory(NestedPrefixBean.class))
            .select("select i nested_i, s nested_s from bean")
            .mapTo(NestedPrefixBean.class)
            .findOnly();
        assertThat(result.nested.s).isEqualTo("3");
        assertThat(result.nested.i).isEqualTo(2);
    }

    @Test
    public void nestedPrefixParametersStrict() {
        Handle handle = dbRule.getSharedHandle();
        handle.getConfig(ReflectionMappers.class).setStrictMatching(true);
        handle.registerRowMapper(ConstructorMapper.factory(NestedPrefixBean.class));

        assertThat(handle
            .createQuery("select i nested_i, s nested_s from bean")
            .mapTo(NestedPrefixBean.class)
            .findOnly())
            .extracting("nested.s", "nested.i")
            .containsExactly("3", 2);

        assertThatThrownBy(() -> handle
            .createQuery("select i nested_i, s nested_s, 1 as other from bean")
            .mapTo(NestedPrefixBean.class)
            .findOnly())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("could not match parameters for columns: [other]");

        assertThatThrownBy(() -> handle
            .createQuery("select i nested_i, s nested_s, 1 as nested_other from bean")
            .mapTo(NestedPrefixBean.class)
            .findOnly())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("could not match parameters for columns: [nested_other]");
    }

    static class NestedPrefixBean {
        final ConstructorBean nested;

        NestedPrefixBean(@Nested("nested") ConstructorBean nested) {
            this.nested = nested;
        }
    }
}
