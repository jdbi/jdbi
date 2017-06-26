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
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestRegisterObjectArgumentFactory {
    @Rule
    public H2DatabaseRule rule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    Handle handle;

    @Before
    public void setUp() {
        handle = rule.getSharedHandle();

        // h2 "other" data type with setObject / getObject uses Java serialization
        handle.execute("create table serialized_types (string_value other, long_value other)");
    }

    @Test
    public void registerFactory() {
        TestDao dao = handle.attach(TestDao.class);

        dao.insertStringValue(StringValue.of("foo"));
        dao.insertStringValue(StringValue.of("bar"));

        List<StringValue> values = handle.select("select string_value from serialized_types")
                .map((rs, ctx) -> (StringValue) rs.getObject("string_value"))
                .list();
        assertThat(values).containsExactly(StringValue.of("foo"), StringValue.of("bar"));
    }

    @Test
    public void registerFactories() {
        TestDao dao = handle.attach(TestDao.class);

        dao.insertValues(StringValue.of("foo"), LongValue.of(10));
        dao.insertValues(StringValue.of("bar"), LongValue.of(20));

        List<ValueTypeEntity> values = handle.select("select * from serialized_types")
                .map((rs, ctx) -> ValueTypeEntity.of(
                        (StringValue) rs.getObject("string_value"),
                        (LongValue) rs.getObject("long_value")))
                .list();

        assertThat(values).containsExactly(
                ValueTypeEntity.of(StringValue.of("foo"), LongValue.of(10)),
                ValueTypeEntity.of(StringValue.of("bar"), LongValue.of(20)));
    }

    public interface TestDao {
        @SqlUpdate("insert into serialized_types (string_value) values (?)")
        @RegisterObjectArgumentFactory(StringValue.class)
        void insertStringValue(StringValue value);

        @SqlUpdate("insert into serialized_types (string_value, long_value) values (?, ?)")
        @RegisterObjectArgumentFactory(StringValue.class)
        @RegisterObjectArgumentFactory(LongValue.class)
        void insertValues(StringValue stringValue, LongValue longValue);
    }

}
