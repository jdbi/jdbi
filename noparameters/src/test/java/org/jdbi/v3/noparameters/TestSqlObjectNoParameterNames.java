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
package org.jdbi.v3.noparameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assume.assumeFalse;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestSqlObjectNoParameterNames {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    Handle h;

    @Before
    public void setUp() throws Exception {
        assumeFalse(Dao.class.getMethod("getByPositionalId", int.class).getParameters()[0].isNamePresent());

        h = dbRule.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'Elsie Hughes')");
    }

    @Test
    public void positionalParameterWithParameterNamesCompiled() throws Exception {
        assertThat(h.attach(Dao.class).getByPositionalId(1)).isEqualTo(new Something(1, "Elsie Hughes"));
    }

    @Test
    public void namedParameterWithNoParameterNamesCompiledOrSupplied() throws Exception {
        assertThatThrownBy(() -> h.attach(Dao.class).getByNamedId(1))
                .isInstanceOf(UnableToExecuteStatementException.class)
                .hasMessageContaining("no named parameter matches 'id'");
    }

    @RegisterBeanMapper(Something.class)
    public interface Dao {
        @SqlQuery("select * from something where id = ?")
        Something getByPositionalId(int id);

        @SqlQuery("select * from something where id = :id")
        Something getByNamedId(int id);
    }
}
