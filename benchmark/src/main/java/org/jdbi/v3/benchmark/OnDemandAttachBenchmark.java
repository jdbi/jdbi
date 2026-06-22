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
package org.jdbi.v3.benchmark;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
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

/**
 * Exercises the per-{@code attach} configuration derivation cost.
 *
 * <p>{@link Jdbi#onDemand(Class)} (and {@link Jdbi#withExtension}) re-attaches the extension on every
 * method call against the same, stable {@code Jdbi}-level configuration. Each attach derives an
 * instance configuration plus one configuration per extension method, each a full
 * {@code ConfigRegistry.createCopy()}. The {@code ManyMethodDao} below has many methods so that the
 * {@code 1 + methodCount} copies dominate, making the effect of caching the derived configurations
 * visible. {@link #handleAttach()} attaches against a per-{@link Handle} configuration (a one-shot
 * attach source that does not benefit from the cache) and is included to confirm there is no
 * regression on that path.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@Measurement(time = 5, iterations = 5)
@Warmup(time = 5, iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
public class OnDemandAttachBenchmark {

    private Jdbi jdbi;
    private Handle handle;

    @Setup(Level.Iteration)
    public void setUp() {
        jdbi = Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=10");
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.registerRowMapper(Data.class, new DataMapper());

        handle = jdbi.open();
        handle.execute("drop table if exists tbl");
        handle.execute("create table tbl (id identity, name varchar, description varchar)");
        handle.execute("insert into tbl (id, name, description) values (0, 'name', 'description')");
    }

    @TearDown(Level.Iteration)
    public void close() {
        if (handle != null) {
            handle.close();
            handle = null;
        }
        jdbi = null;
    }

    /**
     * On-demand style attach: re-attaches against the stable {@code Jdbi} configuration on every call,
     * so the derived configurations can be cached and reused.
     */
    @Benchmark
    public Data onDemandAttach() {
        return jdbi.withExtension(ManyMethodDao.class, dao -> dao.getById(0));
    }

    /**
     * Opens a fresh {@link Handle} per call and attaches against its per-handle configuration — a
     * one-shot attach source (a new configuration each call) that does <em>not</em> reuse cached
     * configurations. This mirrors the typical open-handle-per-unit-of-work pattern and confirms there
     * is no regression on the path the cache cannot help.
     */
    @Benchmark
    public Data handleAttach() {
        try (Handle h = jdbi.open()) {
            return h.attach(ManyMethodDao.class).getById(0);
        }
    }

    @RegisterRowMapper(DataMapper.class)
    public interface ManyMethodDao {

        @SqlQuery("select * from tbl where id = :id")
        Data getById(@Bind("id") long id);

        @SqlQuery("select * from tbl order by id")
        List<Data> listAll();

        @SqlQuery("select * from tbl where name = :name")
        Data getByName(@Bind("name") String name);

        @SqlQuery("select * from tbl where description = :description")
        Data getByDescription(@Bind("description") String description);

        @SqlQuery("select count(*) from tbl")
        long count();

        @SqlQuery("select name from tbl where id = :id")
        String nameById(@Bind("id") long id);

        @SqlQuery("select description from tbl where id = :id")
        String descriptionById(@Bind("id") long id);

        @SqlQuery("select id from tbl order by id")
        List<Long> listIds();

        @SqlQuery("select * from tbl where id > :id")
        List<Data> listAfter(@Bind("id") long id);

        @SqlQuery("select * from tbl where id < :id")
        List<Data> listBefore(@Bind("id") long id);
    }

    public static class Data {

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
