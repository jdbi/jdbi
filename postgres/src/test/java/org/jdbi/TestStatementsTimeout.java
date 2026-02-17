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
package org.jdbi;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.core.Handle;
import org.jdbi.core.result.ResultIterable;
import org.jdbi.core.statement.Query;
import org.jdbi.core.statement.SqlStatements;
import org.jdbi.core.statement.UnableToExecuteStatementException;
import org.jdbi.postgres.PostgresPlugin;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestStatementsTimeout {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg).withPlugins(new SqlObjectPlugin(), new PostgresPlugin());

    private Handle h;

    @BeforeEach
    public void setUp() {
        h = pgExtension.openHandle();
    }

    @AfterEach
    public void tearDown() {
        h.close();
    }

    @Test
    public void testTimeout() {
        h.getConfig(SqlStatements.class).setQueryTimeout(2);

        assertThatCode(h.createQuery("select pg_sleep(1)").mapTo(String.class)::one)
            .doesNotThrowAnyException();

        try (Query query = h.createQuery("select pg_sleep(3)")) {
            ResultIterable<String> iterable = query.mapTo(String.class);

            assertThatThrownBy(iterable::one)
            .isInstanceOf(UnableToExecuteStatementException.class)
                .hasCauseInstanceOf(PSQLException.class)
                .matches(ex -> PSQLState.QUERY_CANCELED.getState().equals(((PSQLException) ex.getCause()).getSQLState()));
        }
    }
}
