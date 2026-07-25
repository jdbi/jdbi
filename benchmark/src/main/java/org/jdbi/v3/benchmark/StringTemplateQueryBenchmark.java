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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.h2.Driver;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.TemplateEngine;
import org.jdbi.v3.stringtemplate4.StringTemplateEngine;
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
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

/**
 * End-to-end effect of caching StringTemplate compilation: both arms run the same conditional query,
 * differing only in the engine. {@code recompiling} recompiles the template every render, as the engine did
 * before this change; {@code cached} is the current engine, which caches compilation.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
public class StringTemplateQueryBenchmark {

    private static final String SQL =
            "select id from tbl where id = :id"
            + " <if(byName)> and name = :nm <endif>"
            + " order by <sortCol>";

    private Handle recompilingHandle;
    private Handle cachedHandle;

    @Setup(Level.Trial)
    public void setup() {
        Driver.load();
        recompilingHandle = open(new RecompilingStringTemplateEngine());
        cachedHandle = open(new StringTemplateEngine());
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        recompilingHandle.close();
        cachedHandle.close();
    }

    private static Handle open(TemplateEngine engine) {
        Jdbi jdbi = Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=10");
        jdbi.setTemplateEngine(engine);
        Handle handle = jdbi.open();
        handle.execute("create table tbl (id integer primary key, name varchar)");
        handle.execute("insert into tbl (id, name) values (1, 'row one')");
        return handle;
    }

    private static Integer run(Handle handle) {
        return handle.createQuery(SQL)
                .define("byName", Boolean.TRUE)
                .define("sortCol", "id")
                .bind("id", 1)
                .bind("nm", "row one")
                .mapTo(Integer.class)
                .one();
    }

    @Benchmark
    public Integer recompiling() {
        return run(recompilingHandle);
    }

    @Benchmark
    public Integer cached() {
        return run(cachedHandle);
    }

    /** Recompiles a fresh StringTemplate every render, as the engine did before it cached compilation. */
    static final class RecompilingStringTemplateEngine implements TemplateEngine {
        @Override
        public String render(String sql, StatementContext ctx) {
            ST template = new ST(new STGroup(), sql);
            ctx.getAttributes().forEach(template::add);
            return template.render();
        }
    }
}
