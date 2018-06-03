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

import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class TestMistypedNamedParameter {

    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Dao dao;

    @Before
    public void setUp() throws Exception {
        dao = dbRule.getSharedHandle().attach(Dao.class);
        dbRule.getSharedHandle().useTransaction(h -> {
            h.execute("insert into something (id, name) values (1, 'Alice')");
            h.execute("insert into something (id, name) values (2, 'Bob')");
            h.execute("insert into something (id, name) values (3, 'Charles')");
        });
    }

    @Test
    public void testWarnAboutUnmatchedBinding() throws Exception {
        assertThatExceptionOfType(UnableToExecuteStatementException.class)
                .isThrownBy(() -> dao.deleteSomething(2))
                .satisfies(e -> assertThat(e.getMessage())
                        .startsWith("Unable to execute. The query doesn't have named parameters, but provided binding"));
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface Dao {

        @SqlUpdate("delete from something where id = id")
        void deleteSomething(@Bind("id") int id);
    }
}
