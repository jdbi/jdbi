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
package org.jdbi.v3;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jdbi.v3.postgres.PostgresDbRule;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestStatementsTimeout {
    @Rule
    public JdbiRule dbRule = PostgresDbRule.rule();

    private Handle h;

    @Before
    public void setUp() {
        h = dbRule.getHandle();
    }

    @Test
    public void testTimeout() {
        h.getConfig(SqlStatements.class).setQueryTimeout(2);

        assertThatCode(h.createQuery("select pg_sleep(1)").mapTo(String.class)::findOnly)
            .doesNotThrowAnyException();

        assertThatThrownBy(h.createQuery("select pg_sleep(3)").mapTo(String.class)::findOnly)
            .isInstanceOf(UnableToExecuteStatementException.class)
            .hasMessageContaining("canceling statement due to user request");
    }
}
