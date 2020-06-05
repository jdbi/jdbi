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
package org.jdbi.v3.core.result;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementCustomizer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestResultBearing {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    @Before
    public void setUp() {
        Handle h = dbRule.getSharedHandle();
        h.execute("CREATE TABLE reduce (u INT)");
        for (int u = 0; u < 5; u++) {
            h.execute("INSERT INTO reduce VALUES (?)", u);
        }
    }

    @Test
    public void testReduceBiFunction() {
        assertThat(
            dbRule.getSharedHandle().createQuery("SELECT * FROM reduce")
                .mapTo(Integer.class)
                .reduce(0, TestResultBearing::add))
            .isEqualTo(10);
    }

    private List<Integer> foundResult;

    @Test
    public void resultAvailableInConfig() {
        dbRule.getSharedHandle().useTransaction(h -> {
            h.createQuery("select 1 union select 2")
                    .addCustomizer(new StatementCustomizer() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public void afterExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException {
                            foundResult = (List<Integer>) ctx.getConfig(ResultProducers.class).result();
                        }
                    })
                    .mapTo(int.class)
                    .list();
            assertThat(foundResult).containsExactly(1, 2);
        });
    }

    public static Integer add(Integer u, Integer v) {
        return u + v;
    }
}
