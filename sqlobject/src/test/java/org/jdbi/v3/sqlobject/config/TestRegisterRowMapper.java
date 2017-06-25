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
package org.jdbi.v3.sqlobject.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.JoinRow;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestRegisterRowMapper {
    @Rule
    public H2DatabaseRule rule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    Handle handle;

    @Before
    public void setUp() {
        handle = rule.getSharedHandle();

        handle.execute("create table column_mappers (string_value varchar, long_value integer)");
        handle.execute("insert into column_mappers (string_value, long_value) values (?, ?)", "foo", 1L);
        handle.execute("insert into column_mappers (string_value, long_value) values (?, ?)", "bar", 2L);
    }

    @Test
    public void registerColumnMappers() {
        TestDao dao = handle.attach(TestDao.class);
        assertThat(dao.listX()).containsExactly(StringValue.of("foo"), StringValue.of("bar"));
        assertThat(dao.listY()).containsExactly(LongValue.of(1L), LongValue.of(2L));
        List<JoinRow> joinRows = dao.list();
        assertThat(joinRows)
                .extracting(row -> row.get(StringValue.class))
                .containsExactly(StringValue.of("foo"), StringValue.of("bar"));
        assertThat(joinRows)
                .extracting(row -> row.get(LongValue.class))
                .containsExactly(LongValue.of(1L), LongValue.of(2L));
    }

    public interface TestDao {
        @SqlQuery("select string_value from column_mappers")
        @RegisterRowMapper(StringValueMapper.class)
        List<StringValue> listX();

        @SqlQuery("select long_value from column_mappers")
        @RegisterRowMapper(LongValueMapper.class)
        List<LongValue> listY();

        @SqlQuery("select * from column_mappers")
        @RegisterRowMapper(StringValueMapper.class)
        @RegisterRowMapper(LongValueMapper.class)
        @RegisterJoinRowMapper({StringValue.class, LongValue.class})
        List<JoinRow> list();
    }
}
