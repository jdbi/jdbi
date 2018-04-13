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
package org.jdbi.v3.sqlobject.customizer;

import java.util.List;
import java.util.Map;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.MapMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestMaxRows {
    private static final String QUERY = "select bar from foo";
    private static final String CREATE_INSERT = "create table foo(bar int primary key);" +
        "insert into foo(bar) values(1);" +
        "insert into foo(bar) values(2);" +
        "insert into foo(bar) values(3);";
    private static final int ROWS = 1;

    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        handle = dbRule.openHandle();
    }

    @Test
    public void testMethodWrong() {
        assertThatThrownBy(() -> handle.attach(FooMethodWrong.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no value given");
    }

    @Test
    public void testParamWrong() {
        assertThatThrownBy(() -> handle.attach(FooParamWrong.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("value won't do anything");
    }

    @Test
    public void testMethodRight() {
        FooMethodRight sqlObject = handle.attach(FooMethodRight.class);

        handle.createScript(CREATE_INSERT).execute();

        assertThat(sqlObject.bar()).hasSize(ROWS);
    }

    @Test
    public void testParamRight() {
        FooParamRight sqlObject = handle.attach(FooParamRight.class);

        handle.createScript(CREATE_INSERT).execute();

        assertThat(sqlObject.bar(ROWS)).hasSize(ROWS);
    }

    @Test
    public void testControlGroup() {
        NoMaxRows sqlObject = handle.attach(NoMaxRows.class);

        handle.createScript(CREATE_INSERT).execute();

        assertThat(sqlObject.bar()).hasSize(3);
    }

    public interface FooMethodWrong extends SqlObject {
        @MaxRows
        @SqlQuery(QUERY)
        List<Map<String, Object>> bar();
    }

    public interface FooParamWrong extends SqlObject {
        @SqlQuery(QUERY)
        List<Map<String, Object>> bar(@MaxRows(ROWS) int rows);
    }

    public interface FooMethodRight extends SqlObject {
        @MaxRows(ROWS)
        @SqlQuery(QUERY)
        @RegisterRowMapper(MapMapper.class)
        List<Map<String, Object>> bar();
    }

    public interface FooParamRight extends SqlObject {
        @SqlQuery(QUERY)
        @RegisterRowMapper(MapMapper.class)
        List<Map<String, Object>> bar(@MaxRows int rows);
    }

    public interface NoMaxRows extends SqlObject {
        @SqlQuery(QUERY)
        @RegisterRowMapper(MapMapper.class)
        List<Map<String, Object>> bar();
    }
}
