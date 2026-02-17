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
package org.jdbi.benchmark.sqlobject;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.customizer.BindBean;
import org.jdbi.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

public abstract class BaseSqlObjectV3Benchmark extends AbstractSqlObjectBenchmark {
    private Jdbi jdbi;
    protected Handle handle;
    private DaoV3 dao;
    private long rowOne;

    protected abstract Jdbi createJdbi();
    protected abstract void createTable();

    private void insertRow() {
        rowOne = handle.attach(DaoV3.class)
                .insertTestDataGetKeyBean(new TestContent("row one", "the first row"));
    }

    @Setup(Level.Iteration)
    public void setup() throws Throwable {
        jdbi = createJdbi();
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.registerRowMapper(new DaoV3.TestDataMapper());

        handle = jdbi.open();
        dao = handle.attach(DaoV3.class);
        createTable();
        insertRow();
    }

    @TearDown(Level.Iteration)
    public void close() {
        if (handle != null) {
            handle.close();
            handle = null;
        }
        jdbi = null;
    }

    @Benchmark
    public DaoV3 attach() {
        return handle.attach(DaoV3.class);
    }

    @Benchmark
    public long fluentInsertGeneratedKeyBindBean() {
        return handle.createUpdate(INSERT)
                     .bindBean(TestContent.TEST_CONTENT)
                     .executeAndReturnGeneratedKeys()
                     .mapTo(long.class)
                     .one();
    }

    @Benchmark
    public long sqlobjectInsertGeneratedKeyBindBean() {
        return dao.insertTestDataGetKeyBean(TestContent.TEST_CONTENT);
    }

    @Benchmark
    public int sqlobjectInsertRowCountBindBean() {
        return dao.insertTestDataRowCountBean(TestContent.TEST_CONTENT);
    }

    @Benchmark
    public long fluentInsertGeneratedKeyValues() {
        return handle.createUpdate(INSERT)
                     .bind("name", TestContent.TEST_CONTENT.getName())
                     .bind("description", TestContent.TEST_CONTENT.getDescription())
                     .executeAndReturnGeneratedKeys()
                     .mapTo(long.class)
                     .one();
    }

    @Benchmark
    public long sqlobjectInsertGeneratedKeyValues() {
        return dao.insertTestDataGetKeyValues(
                    TestContent.TEST_CONTENT.getName(), TestContent.TEST_CONTENT.getDescription());
    }

    @Benchmark
    public int sqlobjectInsertRowCountValues() {
        return dao.insertTestDataRowCountValues(
                    TestContent.TEST_CONTENT.getName(), TestContent.TEST_CONTENT.getDescription());
    }

    @Benchmark
    public TestData fluentSelectOne() {
        return handle.createQuery(SELECT)
                     .bind("id", rowOne)
                     .mapTo(TestData.class)
                     .one();
    }

    @Benchmark
    public TestData sqlobjectSelectOne() {
        return dao.getTestData(rowOne);
    }

    public interface DaoV3 {
        @GetGeneratedKeys
        @SqlUpdate(INSERT)
        long insertTestDataGetKeyBean(@BindBean TestContent testContent);

        @SqlUpdate(INSERT)
        int insertTestDataRowCountBean(@BindBean TestContent testContent);

        @GetGeneratedKeys
        @SqlUpdate(INSERT)
        long insertTestDataGetKeyValues(@Bind("name") String name, @Bind("description") String description);

        @SqlUpdate(INSERT)
        int insertTestDataRowCountValues(@Bind("name") String name, @Bind("description") String description);

        @SqlQuery(SELECT)
        TestData getTestData(@Bind("id") long id);

        class TestDataMapper extends BaseTestDataMapper implements RowMapper<TestData> {
            @Override
            public TestData map(final ResultSet r, final StatementContext ctx) throws SQLException {
                return mapInternal(r);
            }
        }
    }
}
