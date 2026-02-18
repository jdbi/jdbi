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
package org.jdbi.sqlobject;

import java.util.List;
import java.util.Objects;

import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.mapper.Nested;
import org.jdbi.core.mapper.reflect.ConstructorMapper;
import org.jdbi.core.mapper.reflect.JdbiConstructor;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class NestedBeanTest {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    public interface BeanDao {
        @SqlQuery("select intValue as nested_val, name as nested_name, id from something order by id")
        List<MainBeanOne> getOneBeans();

        @SqlQuery("select intValue as val, name as nested_name, id from something order by id")
        List<MainBeanTwo> getTwoBeans();
    }

    public static class MainBeanOne {
        public final int id;
        public final NestedBeanOne bean;

        public MainBeanOne(int id, @Nested("nested") NestedBeanOne bean) {
            this.id = id;
            this.bean = bean;
        }

        public int getId() {
            return id;
        }

        public NestedBeanOne getBean() {
            return bean;
        }
    }

    public static class NestedBeanOne {
        public final Integer val;
        public final String name;

        public NestedBeanOne(Integer val, String name) {
            this.val = val;
            this.name = name;
        }

        public Integer getVal() {
            return val;
        }

        public String getName() {
            return name;
        }
    }

    public static class MainBeanTwo {
        public final int id;
        public final Integer val;
        public final NestedBeanTwo bean;

        public MainBeanTwo(int id, Integer val, @Nested("nested") NestedBeanTwo bean) {
            this.id = id;
            this.val = val;
            this.bean = bean;
        }

        public int getId() {
            return id;
        }

        public Integer getVal() {
            return val;
        }

        public NestedBeanTwo getBean() {
            return bean;
        }
    }

    public static class NestedBeanTwo {

        @JdbiConstructor
        public static NestedBeanTwo create(String name) {
            return new NestedBeanTwo(name);
        }

        public final String name;

        private NestedBeanTwo(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            NestedBeanTwo that = (NestedBeanTwo) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }
    }

    @BeforeEach
    public void setUp() {
        final Jdbi jdbi = h2Extension.getJdbi();
        jdbi.registerRowMapper(ConstructorMapper.factory(MainBeanOne.class));
        jdbi.registerRowMapper(ConstructorMapper.factory(NestedBeanOne.class));
        jdbi.registerRowMapper(ConstructorMapper.factory(MainBeanTwo.class));
        jdbi.registerRowMapper(ConstructorMapper.factory(NestedBeanTwo.class));

        final Handle h = h2Extension.getSharedHandle();
        h.execute("insert into something(intValue, name) values(1, 'Duke')");
        h.execute("insert into something(intValue, name) values(null, null)");
    }

    @Test
    public void testMapBeanOne() {
        final Jdbi jdbi = h2Extension.getJdbi();

        var beans = jdbi.withExtension(BeanDao.class, BeanDao::getOneBeans);

        assertThat(beans)
            .extracting("id", "bean.name", "bean.val")
            .containsExactly(
                tuple(1, "Duke", 1),
                tuple(2, null, null));
    }

    @Test
    public void testMapBeanTwo() {
        final Jdbi jdbi = h2Extension.getJdbi();

        var beans = jdbi.withExtension(BeanDao.class, BeanDao::getTwoBeans);

        assertThat(beans)
            .extracting("id", "bean.name", "val")
            .containsExactly(
                tuple(1, "Duke", 1),
                tuple(2, null, null));
    }
}
