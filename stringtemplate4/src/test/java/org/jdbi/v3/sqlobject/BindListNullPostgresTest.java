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
package org.jdbi.v3.sqlobject;

import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.rule.PgDatabaseRule;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.stringtemplate4.TestStringTemplateSqlLocator.SomethingMapper;
import org.jdbi.v3.stringtemplate4.UseStringTemplateEngine;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BindListNullPostgresTest {
    @Rule
    public PgDatabaseRule dbRule = new PgDatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @Before
    public void init() {
        handle = dbRule.openHandle();

        handle.execute("create table something (id int primary key, name varchar(100))");
        handle.execute("insert into something(id, name) values(1, null)");
        handle.execute("insert into something(id, name) values(2, 'bla')");
        handle.execute("insert into something(id, name) values(3, 'null')");
        handle.execute("insert into something(id, name) values(4, '')");
    }

    @After
    public void exit() {
        handle.execute("drop table something");
        handle.close();
    }

    @Test
    public void testSomethingByIterableHandleNullWithNull() {
        final SomethingByIterableHandleNull s = handle.attach(SomethingByIterableHandleNull.class);

        final List<Something> out = s.get(null);

        assertThat(out).isEmpty();
    }

    @Test
    public void testSomethingByIterableHandleNullWithEmptyList() {
        final SomethingByIterableHandleNull s = handle.attach(SomethingByIterableHandleNull.class);

        final List<Something> out = s.get(new ArrayList<Object>());

        assertThat(out).isEmpty();
    }

    @UseStringTemplateEngine
    @RegisterRowMapper(SomethingMapper.class)
    public interface SomethingByIterableHandleNull {
        @SqlQuery("select id, name from something where name in (<names>)")
        List<Something> get(@BindList(value = "names", onEmpty = BindList.EmptyHandling.NULL) Iterable<Object> ids);
    }
}
