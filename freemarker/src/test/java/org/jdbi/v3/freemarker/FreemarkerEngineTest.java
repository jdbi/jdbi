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
package org.jdbi.v3.freemarker;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.freemarker.FreemarkerSqlLocatorTest.SomethingMapper;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FreemarkerEngineTest {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withSomething().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @Before
    public void setUp() {
        handle = dbRule.getSharedHandle();
    }

    @Test
    public void testFindBeanWithBind() {
        handle.execute("insert into something (id, name) values (6, 'Martin Freeman')");

        Something s = handle.attach(Wombat.class).findByBoundId(6L);
        assertThat(s.getName()).isEqualTo("Martin Freeman");
    }

    @Test
    public void testFindBeanWithDefine() {
        handle.execute("insert into something (id, name) values (6, 'Peter Jackson')");

        Something s = handle.attach(Wombat.class).findByDefinedId(6L);
        assertThat(s.getName()).isEqualTo("Peter Jackson");
    }

    @Test
    public void testFindNamesWithDefinedIds() {
        handle.execute("insert into something (id, name) values (6, 'Jack')");
        handle.execute("insert into something (id, name) values (7, 'Wolf')");

        List<String> s = handle.attach(Wombat.class).findNamesByDefinedIds(Arrays.asList(6L, 7L));
        assertThat(s).containsExactly("Jack", "Wolf");
    }

    @Test
    public void testFindNamesConditionalExecutionWithNullValue() {
        handle.execute("insert into something (id, name) values (6, 'Jack')");
        handle.execute("insert into something (id, name) values (7, 'Wolf')");

        List<String> s = handle.attach(Wombat.class).findNamesByDefinedIdsOrAll(null);
        assertThat(s).containsExactly("Jack", "Wolf");
    }

    @Test
    public void testFindNamesWithConditionalExecutionWithNonNullValue() {
        handle.execute("insert into something (id, name) values (6, 'Jack')");
        handle.execute("insert into something (id, name) values (7, 'Wolf')");

        List<String> s = handle.attach(Wombat.class).findNamesByDefinedIdsOrAll(Collections.singletonList(6L));
        assertThat(s).containsExactly("Jack");
    }

    @UseFreemarkerEngine
    @RegisterRowMapper(SomethingMapper.class)
    public interface Wombat {

        @SqlQuery("select * from something where id = :id")
        Something findByBoundId(@Bind("id") Long id);

        @SqlQuery("select * from something where id = ${id}")
        Something findByDefinedId(@Define("id") Long id);

        @SqlQuery("select name from something where id in (${ids?join(\",\")})")
        List<String> findNamesByDefinedIds(@Define("ids") List<Long> ids);

        @SqlQuery("select name from something <#if ids??> where id in (${ids?join(\",\")}) </#if>")
        List<String> findNamesByDefinedIdsOrAll(@Define("ids") List<Long> ids);

    }
}
