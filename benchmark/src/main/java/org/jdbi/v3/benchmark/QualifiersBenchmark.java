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

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.qualifier.NVarchar;
import org.jdbi.v3.core.qualifier.Qualifiers;
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
public class QualifiersBenchmark {

    private JdbiRule db;
    private Jdbi jdbi;
    private Qualifiers qualifiers;

    @Setup
    public void setup() throws Throwable {
        db = JdbiRule.h2();
        db.before();
        jdbi = db.getJdbi();
        jdbi.registerRowMapper(BeanMapper.factory(UnqualifiedBean.class));
        jdbi.registerRowMapper(BeanMapper.factory(QualifiedBean.class));
        qualifiers = new Qualifiers();
    }

    @TearDown
    public void close() {
        db.after();
    }

    @Benchmark
    public Set<Annotation> getQualifiersUnannotated() {
        return qualifiers.findFor(UnqualifiedBean.class);
    }

    @Benchmark
    public Set<Annotation> getQualifiersAnnotated() {
        return qualifiers.findFor(QualifiedBean.class);
    }

    @Benchmark
    public UnqualifiedBean mapUnqualifiedBean() {
        return jdbi.withHandle(h -> h.createQuery("select 'a' as a, 'b' as b, 'c' as c").mapTo(UnqualifiedBean.class).findOnly());
    }

    @Benchmark
    public QualifiedBean mapQualifiedBean() {
        return jdbi.withHandle(h -> h.createQuery("select 'a' as a, 'b' as b, 'c' as c").mapTo(QualifiedBean.class).findOnly());
    }

    @SuppressWarnings("PMD.DataClass")
    public static class BaseBean {
        private String a;
        private String b;
        private String c;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }

        public String getB() {
            return b;
        }

        public void setB(String b) {
            this.b = b;
        }

        public String getC() {
            return c;
        }

        public void setC(String c) {
            this.c = c;
        }
    }

    public static class UnqualifiedBean extends BaseBean {}

    @NVarchar
    public static class QualifiedBean extends BaseBean {}
}
