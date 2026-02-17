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
package org.jdbi.sqlobject.config;

import java.util.List;

import org.jdbi.core.Handle;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRegisterColumnMapperFactory {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin());

    Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();

        handle.execute("create table column_mappers (string_value varchar, long_value integer)");
        handle.execute("insert into column_mappers (string_value, long_value) values (?, ?)", "foo", 1L);
        handle.execute("insert into column_mappers (string_value, long_value) values (?, ?)", "bar", 2L);
    }

    @Test
    public void registerColumnMapperFactories() {
        TestDao dao = handle.attach(TestDao.class);
        assertThat(dao.listStringValues()).containsExactly(StringValue.of("foo"), StringValue.of("bar"));
        assertThat(dao.listLongValues()).containsExactly(LongValue.of(1L), LongValue.of(2L));
        assertThat(dao.list()).containsExactly(
                new ValueTypeEntity(StringValue.of("foo"), LongValue.of(1L)),
                new ValueTypeEntity(StringValue.of("bar"), LongValue.of(2L)));
    }

    public interface TestDao {
        @SqlQuery("select string_value from column_mappers")
        @RegisterColumnMapperFactory(StringValueColumnMapperFactory.class)
        List<StringValue> listStringValues();

        @SqlQuery("select long_value from column_mappers")
        @RegisterColumnMapperFactory(LongValueColumnMapperFactory.class)
        List<LongValue> listLongValues();

        @SqlQuery("select * from column_mappers")
        @RegisterColumnMapperFactory(StringValueColumnMapperFactory.class)
        @RegisterColumnMapperFactory(LongValueColumnMapperFactory.class)
        @RegisterConstructorMapper(ValueTypeEntity.class)
        List<ValueTypeEntity> list();
    }

}
