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
package org.jdbi.v3.core;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Measurement(time = 5)
@Warmup(time = 2)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
public class EnumBenchmark {
    private JdbiRule db;
    private Jdbi jdbi;

    @Setup
    public void setup() throws Throwable {
        db = JdbiRule.h2();
        db.before();
        jdbi = db.getJdbi();
        Random r = new Random();
        jdbi.useHandle(h -> {
            h.execute("create table sensitive (value varchar)");
            h.execute("create table insensitive (value varchar)");
            SwedishChef[] values = SwedishChef.values();
            for (int i = 0; i < 1000; i++) {
                SwedishChef element = values[r.nextInt(values.length)];
                h.execute("insert into sensitive(value) values(?)", element);
                h.execute("insert into insensitive(value) values(?)", randomizeCase(r, element.name()));
            }
        });
    }

    private String randomizeCase(Random r, String name) {
        return name.chars()
            .map(c -> r.nextBoolean() ? Character.toUpperCase(c) : Character.toLowerCase(c))
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
    }

    @TearDown
    public void close() {
        db.after();
    }

    @Benchmark
    public List<SwedishChef> mapEnumCaseInsensitive() {
        return run("insensitive");
    }

    @Benchmark
    public List<SwedishChef> mapEnumCaseSensitive() {
        return run("sensitive");
    }

    private List<SwedishChef> run(String table) {
        return jdbi.withHandle(h -> h.createQuery("select value from " + table).mapTo(SwedishChef.class).list());
    }
    public enum SwedishChef {
        NOLL, ETT, TVA, TRE, FYRA, FEM, SEX, SJU, ATTA, NIO, TIO
    }
}
