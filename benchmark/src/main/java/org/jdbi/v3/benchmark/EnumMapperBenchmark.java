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

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.jdbi.v3.core.Jdbi;
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
@BenchmarkMode(Mode.AverageTime)
@Measurement(time = 5)
@Warmup(time = 2)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
public class EnumMapperBenchmark {

    private Random random;
    private JdbiRule db;
    private Jdbi jdbi;

    @Setup
    public void setup() throws Throwable {
        random = new Random();

        db = JdbiRule.h2();
        db.before();
        jdbi = db.getJdbi();

        jdbi.useHandle(handle -> {
            handle.execute("create table exact_name (name varchar)");
            handle.execute("create table random_case (name varchar)");

            Tribble[] values = Tribble.values();
            for (int i = 0; i < 1000; i++) {
                Tribble tribble = values[random.nextInt(values.length)];

                handle.execute("insert into exact_name (name) values (?)", tribble.name());
                handle.execute("insert into random_case (name) values (?)", randomizeCase(tribble.name()));
            }
        });
    }

    @TearDown
    public void close() {
        db.after();
    }

    @Benchmark
    public List<Tribble> mapExactCase() {
        return jdbi.withHandle(h -> h.select("select name from exact_name").mapTo(Tribble.class).list());
    }

    @Benchmark
    public List<Tribble> mapRandomCase() {
        return jdbi.withHandle(h -> h.select("select name from random_case").mapTo(Tribble.class).list());
    }

    public enum Tribble {
        FOO,
        BAR,
        BAZ,
        QUX,
        QUUX,
        QUUZ,
        CORGE,
        GRAULT,
        GARPLY,
        WALDO,
        FRED,
        PLUGH,
        XYZZY,
        THUD,
        WIBBLE,
        WOBBLE,
        WUBBLE,
        FLOB
    }

    private String randomizeCase(String s) {
        StringBuilder b = new StringBuilder(s.length());

        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            b.append(random.nextBoolean() ? Character.toUpperCase(ch) : Character.toLowerCase(ch));
        }

        return b.toString();
    }
}
