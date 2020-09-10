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
package org.jdbi.v3.benchmark.sqlobject;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.util.LongColumnMapper;

public abstract class BaseSqlObjectV2Benchmark extends AbstractSqlObjectBenchmark {
    private DBI jdbi;
    protected Handle handle;
    private DaoV2 dao;
    private long rowOne;

    protected abstract DBI createJdbi();
    protected abstract void createTable();

    @Setup(Level.Iteration)
    public void setup() throws Throwable {
        jdbi = createJdbi();
        jdbi.registerMapper(new DaoV2.TestDataMapper());

        handle = jdbi.open();
        dao = handle.attach(DaoV2.class);
        createTable();
        insertRow();
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        if (handle != null) {
            handle.close();
            handle = null;
        }
        jdbi = null;
    }

    private void insertRow() {
        rowOne = dao.insertTestDataGetKeyBean(new TestContent("row one", "the first row"));
    }

    @Benchmark
    public DaoV2 attach() {
        return handle.attach(DaoV2.class);
    }

    @Benchmark
    public long fluentInsertGeneratedKeyBindBean() {
        return handle.createStatement(INSERT)
                    .bindFromProperties(TestContent.TEST_CONTENT)
                    .executeAndReturnGeneratedKeys(LongColumnMapper.WRAPPER)
                    .first();
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
        return handle.createStatement(INSERT)
                     .bind("name", TestContent.TEST_CONTENT.getName())
                     .bind("description", TestContent.TEST_CONTENT.getDescription())
                     .executeAndReturnGeneratedKeys(LongColumnMapper.WRAPPER)
                     .first();
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
                     .first();
    }

    @Benchmark
    public TestData sqlobjectSelectOne() {
        return dao.getTestData(rowOne);
    }

    public interface DaoV2 extends Closeable {
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

        @Override
        void close();

        class TestDataMapper extends BaseTestDataMapper implements ResultSetMapper<TestData> {
            @Override
            public TestData map(final int row, final ResultSet r, final StatementContext ctx) throws SQLException {
                return mapInternal(r);
            }
        }
    }
}
