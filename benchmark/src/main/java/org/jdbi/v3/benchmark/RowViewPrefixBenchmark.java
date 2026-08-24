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

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.testing.JdbiRule;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Compares row mapper lookup and RowView.getRow with and without a mapper prefix.
 * The prefixed and unprefixed benchmarks run the same query and resolve to the same
 * mapper instance, so any difference is the cost of the lookup path itself.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@Measurement(time = 5)
@Warmup(time = 2)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
public class RowViewPrefixBenchmark {
    private static final int ROWS = 1000;

    private JdbiRule db;
    private Handle handle;
    private RowMappers rowMappers;

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
            .include(RowViewPrefixBenchmark.class.getSimpleName())
            .forks(0)
            .build();
        new Runner(options).run();
    }

    @Setup
    public void setup() throws Throwable {
        db = JdbiRule.h2();
        db.before();
        handle = db.getHandle();
        handle.execute("create table contacts (a_id int, a_name varchar(50), b_id int, b_name varchar(50))");
        for (int i = 0; i < ROWS; i++) {
            handle.execute("insert into contacts values (?, ?, ?, ?)", i, "a" + i, i, "b" + i);
        }
        handle.registerRowMapper(BeanMapper.factory(Contact.class, "a"));
        handle.registerRowMapper(BeanMapper.factory(Contact.class, "b"));
        rowMappers = handle.getConfig(RowMappers.class);
        // populate the lookup caches so the benchmarks measure the steady state
        rowMappers.findFor(Contact.class);
        rowMappers.findFor((Type) Contact.class, "b");
    }

    @TearDown
    public void close() {
        db.after();
    }

    @Benchmark
    public Optional<RowMapper<?>> findForUnprefixed() {
        return rowMappers.findFor((Type) Contact.class);
    }

    @Benchmark
    public Optional<RowMapper<?>> findForPrefixed() {
        return rowMappers.findFor((Type) Contact.class, "b");
    }

    @Benchmark
    public int reduceRowsUnprefixed() {
        return handle.createQuery("select * from contacts")
            .reduceRows(0, (sum, rv) -> sum + rv.getRow(Contact.class).getId());
    }

    @Benchmark
    public int reduceRowsPrefixed() {
        return handle.createQuery("select * from contacts")
            .reduceRows(0, (sum, rv) -> sum + rv.getRow(Contact.class, "b").getId());
    }

    @Benchmark
    public int reduceRowsTwoPrefixes() {
        return handle.createQuery("select * from contacts")
            .reduceRows(0, (sum, rv) -> sum + rv.getRow(Contact.class, "a").getId() + rv.getRow(Contact.class, "b").getId());
    }

    public static class Contact {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
