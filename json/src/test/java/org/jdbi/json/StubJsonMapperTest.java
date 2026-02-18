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
package org.jdbi.json;

import org.jdbi.core.statement.UnableToCreateStatementException;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StubJsonMapperTest {

    @RegisterExtension
    public final JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin()).withPlugin(new JsonPlugin());

    @Test
    public void defaultFactoriesAreWorkingForSqlObject() {
        h2Extension.getJdbi().useHandle(h -> {
            FooDao dao = h.attach(FooDao.class);

            dao.table();

            assertThatThrownBy(() -> dao.insert(new Foo()))
                .isInstanceOf(UnableToCreateStatementException.class)
                .hasMessageContaining("need to install")
                .hasMessageContaining("a JsonMapper");

            assertThatThrownBy(dao::get)
                .isInstanceOf(UnableToCreateStatementException.class)
                .hasMessageContaining("need to install")
                .hasMessageContaining("a JsonMapper");
        });
    }

    public static class Foo {}

    private interface FooDao {
        @SqlUpdate("create table json(val varchar)")
        void table();

        @SqlUpdate("insert into json(val) values(:json)")
        void insert(@Json Foo json);

        @SqlQuery("select '{}'")
        @Json
        Foo get();
    }
}
