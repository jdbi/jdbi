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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@Measurement(time = 10, iterations = 10)
@Warmup(time = 10, iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(4)
public class SqlObjectOperationBenchmark {

    private Jdbi jdbi;
    private Handle handle;
    private long totalCount;

    @Setup(Level.Iteration)
    public void setUp() {
        jdbi = Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=10");
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.registerRowMapper(Data.class, new DataMapper());

        handle = jdbi.open();
        handle.execute("drop table if exists tbl");
        handle.execute("create table tbl (id identity, name varchar, description varchar)");

        this.totalCount = insertRows(1000);
    }

    private int insertRows(int count) {

        int rowCount = 0;
        while (rowCount < count) {
            int batchSize = 100;
            try (PreparedBatch batch = handle.prepareBatch("INSERT INTO tbl (id, name, description) VALUES (:id, :name, :description)")) {
                while (batchSize > 0 && rowCount < count) {
                    batch.bind("id", rowCount)
                            .bind("name", "Name for " + rowCount)
                            .bind("description", "Description for " + rowCount)
                            .add();
                    rowCount++;
                    batchSize--;
                }
                batch.execute();
            }
        }
        return rowCount;
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
    public List<Data> jdbiRegistered() {
        return jdbi.withExtension(NakedDao.class, NakedDao::getData);
    }

    @Benchmark
    public List<Data> methodRegistered() {
        return jdbi.withExtension(RowMapperMethodDao.class, RowMapperMethodDao::getData);
    }

    @Benchmark
    public List<Data> classRegistered() {
        return jdbi.withExtension(RowMapperClassDao.class, RowMapperClassDao::getData);
    }

    public interface NakedDao {

        @SqlQuery("SELECT * FROM tbl")
        List<Data> getData();
    }

    public interface RowMapperMethodDao {

        @RegisterRowMapper(DataMapper.class)
        @SqlQuery("SELECT * FROM tbl")
        List<Data> getData();
    }

    @RegisterRowMapper(DataMapper.class)
    public interface RowMapperClassDao {

        @SqlQuery("SELECT * FROM tbl")
        List<Data> getData();
    }

    static class Data {

        private final int id;
        private final String name;
        private final String description;

        public Data(int id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public int id() {
            return id;
        }

        public String name() {
            return name;
        }

        public String description() {
            return description;
        }
    }

    public static class DataMapper implements RowMapper<Data> {

        @Override
        public Data map(final ResultSet r, final StatementContext ctx) throws SQLException {
            return new Data(r.getInt("id"), r.getString("name"), r.getString("description"));
        }
    }
}
