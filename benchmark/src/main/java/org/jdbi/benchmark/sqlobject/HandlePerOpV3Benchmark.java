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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.h2.Driver;
import org.jdbi.benchmark.sqlobject.BaseSqlObjectV3Benchmark.DaoV3;
import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.sqlobject.SqlObjectPlugin;
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
 * Opens a <em>fresh handle per invocation</em> (the jdbi/jdbi#2992 "one handle per request" scenario) and
 * measures the cost of the handle-boundary configuration copy. With the handle config as a copy-on-write child
 * of the frozen Jdbi root, an unmodified handle shares the root's warm resolvers (mappers, extension metadata)
 * instead of paying a cold {@code createCopy()} on every open. The deterministic metric is
 * {@code gc.alloc.rate.norm} (bytes per operation); A/B the same build with the handle-COW change in vs out.
 * <p>
 * The {@code withHandle} variants matter specifically: the per-callback attach-for-cleanup scope was moved off
 * per-handle config, so a callback handle no longer forks the shared config and now shares the warm resolvers too.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
public class HandlePerOpV3Benchmark {

    private static final String CREATE = "create table tbl (id identity, name varchar, description varchar)";
    private static final String INSERT = "INSERT INTO tbl (name, description) VALUES (:name, :description)";
    private static final String SELECT = "SELECT id, name, description FROM tbl WHERE id = :id";

    static {
        Driver.load();
    }

    private Jdbi jdbi;
    private long rowOne;

    @Setup(Level.Trial)
    public void setup() {
        jdbi = Jdbi.builder("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=10")
                .installPlugin(new SqlObjectPlugin())
                .registerRowMapper(new DaoV3.TestDataMapper())
                .build();

        try (Handle h = jdbi.open()) {
            h.execute(CREATE);
            rowOne = h.createUpdate(INSERT)
                    .bind("name", "row one")
                    .bind("description", "the first row")
                    .executeAndReturnGeneratedKeys()
                    .mapTo(long.class)
                    .one();
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        jdbi = null;
    }

    @Benchmark
    public TestData openFluentSelect() {
        try (Handle h = jdbi.open()) {
            return h.createQuery(SELECT)
                    .bind("id", rowOne)
                    .mapTo(TestData.class)
                    .one();
        }
    }

    @Benchmark
    public TestData withHandleFluentSelect() {
        return jdbi.withHandle(h -> h.createQuery(SELECT)
                .bind("id", rowOne)
                .mapTo(TestData.class)
                .one());
    }

    @Benchmark
    public TestData openAttachSelect() {
        try (Handle h = jdbi.open()) {
            return h.attach(DaoV3.class).getTestData(rowOne);
        }
    }

    @Benchmark
    public TestData withHandleAttachSelect() {
        return jdbi.withHandle(h -> h.attach(DaoV3.class).getTestData(rowOne));
    }
}
