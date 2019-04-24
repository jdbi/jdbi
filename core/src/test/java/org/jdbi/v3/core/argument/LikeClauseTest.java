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
package org.jdbi.v3.core.argument;

import java.util.List;

import org.jdbi.v3.core.rule.SqliteDatabaseRule;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LikeClauseTest {
    @Rule
    public SqliteDatabaseRule db = new SqliteDatabaseRule();

    @Before
    public void before() {
        db.getSharedHandle().createUpdate("create table foo(bar varchar)").execute();
        db.getSharedHandle().prepareBatch("insert into foo(bar) values(:bar)")
            .bind("bar", "bam").add()
            .bind("bar", "gamma").add()
            .bind("bar", ":hello").add()
            .execute();
    }

    @Test
    public void concatenationWorks() {
        List<String> names = db.getSharedHandle().createQuery("select bar from foo where bar like '%' || :hello || '%'")
            .bind("hello", "am")
            .mapTo(String.class)
            .list();

        assertThat(names).containsExactly("bam", "gamma");
    }

    @Test
    public void theIntuitiveWayDoesntWork() {
        Query query = db.getSharedHandle().createQuery("select bar from foo where bar like '%:hello%'")
            .bind("hello", "am");

        assertThatThrownBy(() -> query.mapTo(String.class).one())
            .isInstanceOf(UnableToCreateStatementException.class);

        String name = query
            // this lovely safeguard :)
            .configure(SqlStatements.class, sqls -> sqls.setUnusedBindingAllowed(true))
            .mapTo(String.class)
            .one();

        assertThat(name).isEqualTo(":hello");
    }
}
