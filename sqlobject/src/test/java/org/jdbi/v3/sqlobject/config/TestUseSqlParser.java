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

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.ColonPrefixSqlParser;
import org.jdbi.v3.core.statement.HashPrefixSqlParser;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestUseSqlParser {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @Before
    public void setUp() throws Exception {
        Jdbi db = dbRule.getJdbi();

        // this is the default, but be explicit for sake of clarity in test
        db.setSqlParser(new ColonPrefixSqlParser());
        handle = db.open();
    }

    @Test
    public void testFoo() throws Exception {
        // test will raise exceptions if SQL is bogus -- if it uses the colon prefix form

        Hashed h = handle.attach(Hashed.class);
        h.insert(new Something(1, "Joy"));
        Something s = h.findById(1);
        assertThat(s.getName()).isEqualTo("Joy");
    }

    @UseSqlParser(HashPrefixSqlParser.class)
    @RegisterRowMapper(SomethingMapper.class)
    public interface Hashed {
        @SqlUpdate("insert into something (id, name) values (#id, #name)")
        void insert(@BindBean Something s);

        @SqlQuery("select id, name from something where id = #id")
        Something findById(@Bind("id") int id);

    }

}
