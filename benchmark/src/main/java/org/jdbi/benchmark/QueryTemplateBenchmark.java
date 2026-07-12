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
package org.jdbi.benchmark;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.statement.QueryTemplate;
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
 * Compares the classic per-statement {@code Handle.createQuery} path against a reused
 * {@link QueryTemplate} for the same single-row SELECT. The interesting metric is
 * {@code gc.alloc.rate.norm} (bytes allocated per operation, deterministic): the classic path
 * pays a full {@code ConfigRegistry.createCopy()} plus SQL render/parse on every call, while the
 * template snapshots configuration once at build time and reuses it.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
public class QueryTemplateBenchmark {

    private static final String SELECT = "SELECT name FROM tbl WHERE id = :id";

    private Jdbi jdbi;
    private Handle handle;
    private long rowOne;
    private QueryTemplate<String> template;

    @Setup(Level.Trial)
    public void setup() {
        jdbi = Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=10");
        handle = jdbi.open();
        handle.execute("create table tbl (id integer primary key, name varchar)");
        handle.execute("insert into tbl (id, name) values (1, 'eric')");
        rowOne = 1L;
        // Built once; reused across every benchmark invocation.
        template = jdbi.buildQueryTemplate(SELECT).mapTo(String.class);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        handle.close();
    }

    @Benchmark
    public String classic() {
        return handle.createQuery(SELECT)
            .bind("id", rowOne)
            .mapTo(String.class)
            .one();
    }

    @Benchmark
    public String template() {
        return template.with(handle)
            .bind("id", rowOne)
            .execute()
            .one();
    }
}
