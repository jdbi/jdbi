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

import org.jdbi.v3.core.*;
import org.jdbi.v3.core.extension.ExtensionMethod;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.TimingCollector;
import org.jdbi.v3.sqlobject.statement.BatchChunkSize;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlCall;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class TestTimingCollector {

    @Rule
    public H2DatabaseRule h2DatabaseRule = new H2DatabaseRule().withPlugins();

    private CustomTimingCollector timingCollector = new CustomTimingCollector();
    private DAO dao;
    private Jdbi jdbi;

    @Before
    public void setUp() throws Exception {
        jdbi = h2DatabaseRule.getJdbi();
        jdbi.useHandle(h -> h.execute("CREATE ALIAS custom_insert FOR " +
                "\"org.jdbi.v3.sqlobject.TestTimingCollector.customInsert\";"));
        jdbi.setTimingCollector(timingCollector);
        dao = jdbi.onDemand(DAO.class);
    }


    @Test
    public void testInsert() {
        dao.insert(1, "Brian");
        dao.insert(2, "Jeff");
        assertThat(timingCollector.statementNames).containsExactly("org.jdbi.v3.sqlobject.DAO.insert");
    }

    @Test
    public void testInsertBatch() {
        dao.insertBatch(Arrays.asList(1, 2, 3), Arrays.asList("Mary", "David", "Kate"));
        assertThat(timingCollector.statementNames).containsOnly("org.jdbi.v3.sqlobject.DAO.insertBatch");
    }

    @Test
    public void testCustomInsert() {
        dao.customInsert(1, "Robb");
        dao.customInsert(2, "Greg");

        assertThat(timingCollector.statementNames).containsOnly("org.jdbi.v3.sqlobject.DAO.customInsert");
    }

    @Test
    public void testSqlQuery() {
        AdvancedDAO advancedDAO = jdbi.onDemand(AdvancedDAO.class);
        advancedDAO.insertBatch(Arrays.asList(1, 2, 3), Arrays.asList("Mary", "David", "Kate"));
        String name = advancedDAO.findNameById(3);
        assertThat(name).isEqualTo("Kate");
        assertThat(timingCollector.statementNames).containsOnly(
                "org.jdbi.v3.sqlobject.AdvancedDAO.insertBatch",
                "org.jdbi.v3.sqlobject.AdvancedDAO.findNameById");
    }

    @Test
    public void testRawSql() {
        jdbi.useHandle(h -> {
            PreparedBatch batch = h.prepareBatch("insert into something (id, name) values (?, ?)");
            batch.add(1, "Mary");
            batch.add(2, "David");
            batch.add(3, "Kate");
            batch.execute();
        });
        List<String> names = jdbi.withHandle(h -> h.createQuery("select name from something order by name")
                .mapTo(String.class)
                .list());
        assertThat(names).containsExactly("David", "Kate", "Mary");
        assertThat(timingCollector.statementNames).containsOnly(
                "sql.raw.insert into something (id, name) values (?, ?)",
                "sql.raw.select name from something order by name");
    }

    public static int customInsert(Connection conn, int id, String name) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("insert into something (id, name) values (?, ?)");
        stmt.setInt(1, id);
        stmt.setString(2, name);
        return stmt.executeUpdate();
    }

    private static class CustomTimingCollector implements TimingCollector {

        private Set<String> statementNames = new HashSet<>();
        private SqlObjectStrategy statementNameStrategy = new SqlObjectStrategy();

        @Override
        public void collect(long elapsedTime, StatementContext ctx) {
            statementNames.add(statementNameStrategy.getStatementName(ctx));
        }
    }

    private static class SqlObjectStrategy {

        String getStatementName(StatementContext statementContext) {
            ExtensionMethod extensionMethod = statementContext.getExtensionMethod();
            if (extensionMethod != null) {
                Class<?> type = extensionMethod.getType();
                Method method = extensionMethod.getMethod();

                String group = type.getPackage().getName();
                String name = type.getSimpleName();

                return group + "." + name + "." + method.getName();
            } else {
                return "sql.raw." + statementContext.getRawSql();
            }
        }
    }

    public interface DAO {

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlBatch("insert into something (id, name) values (:id, :name)")
        @BatchChunkSize(2)
        void insertBatch(@Bind("id") List<Integer> ids, @Bind("name") List<String> names);

        @SqlCall("call custom_insert(:id, :name)")
        void customInsert(@Bind("id") int id, @Bind("name") String name);
    }

    public interface AdvancedDAO extends DAO {

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int id);
    }
}
