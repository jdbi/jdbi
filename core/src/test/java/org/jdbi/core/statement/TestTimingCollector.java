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
package org.jdbi.core.statement;

import java.util.ArrayList;
import java.util.List;

import org.jdbi.core.Handle;
import org.jdbi.core.Something;
import org.jdbi.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestTimingCollector {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(H2DatabaseExtension.SOMETHING_INITIALIZER);

    private TTC tc;

    protected Handle openHandle() {
        tc = new TTC();

        h2Extension.getJdbi().getConfig(SqlStatements.class).setTimingCollector(tc);
        return h2Extension.openHandle();
    }

    @Test
    public void testInsert() {
        try (Handle h = openHandle()) {

            String statement = "insert into something (id, name) values (1, 'eric')";
            int c = h.execute(statement);
            assertThat(c).isOne();

            assertThat(tc.getRawStatements()).containsExactly(statement);
            assertThat(tc.getRenderedStatements()).containsExactly(statement);
            assertThat(tc.getParsedStatements()).extracting("sql").containsExactly(statement);
        }
    }

    @Test
    public void testUpdate() {
        try (Handle h = openHandle()) {

            String stmt1 = "insert into something (id, name) values (1, 'eric')";
            String stmt2 = "update something set name = :name where id = :id";
            String stmt3 = "select * from something where id = :id";

            h.execute(stmt1);

            h.createUpdate(stmt2)
                .bind("id", 1)
                .bind("name", "ERIC")
                .execute();

            Something eric = h.createQuery(stmt3)
                .bind("id", 1)
                .mapToBean(Something.class)
                .list().get(0);
            assertThat(eric.getName()).isEqualTo("ERIC");

            assertThat(tc.getRawStatements()).containsExactly(stmt1, stmt2, stmt3);
            assertThat(tc.getRenderedStatements()).containsExactly(stmt1, stmt2, stmt3);
            assertThat(tc.getParsedStatements()).extracting("sql").containsExactly(
                stmt1,
                "update something set name = ? where id = ?",
                "select * from something where id = ?");
        }
    }

    @Test
    public void testBatch() {
        try (Handle h = openHandle()) {

            String insert = "insert into something (id, name) values (:id, :name)";
            h.prepareBatch(insert)
                .bind("id", 1).bind("name", "Eric").add()
                .bind("id", 2).bind("name", "Brian").add()
                .execute();

            String select = "select * from something order by id";
            List<Something> r = h.createQuery(select).mapToBean(Something.class).list();
            assertThat(r.stream().map(Something::getName)).containsExactly("Eric", "Brian");

            assertThat(tc.getRawStatements()).containsExactly(insert, select);
            assertThat(tc.getRenderedStatements()).containsExactly(insert, select);
            assertThat(tc.getParsedStatements()).extracting("sql").containsExactly(
                "insert into something (id, name) values (?, ?)",
                select);
        }
    }

    private static class TTC implements TimingCollector {

        private final List<String> rawStatements = new ArrayList<>();
        private final List<String> renderedStatements = new ArrayList<>();
        private final List<ParsedSql> parsedStatements = new ArrayList<>();

        @Override
        public synchronized void collect(final long elapsedTime, final StatementContext ctx) {
            rawStatements.add(ctx.getRawSql());
            renderedStatements.add(ctx.getRenderedSql());
            parsedStatements.add(ctx.getParsedSql());
        }

        public synchronized List<String> getRawStatements() {
            return rawStatements;
        }

        public List<String> getRenderedStatements() {
            return renderedStatements;
        }

        public synchronized List<ParsedSql> getParsedStatements() {
            return parsedStatements;
        }
    }
}
