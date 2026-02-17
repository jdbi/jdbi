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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jdbi.core.Jdbi;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.mapper.reflect.BeanMapper;
import org.jdbi.core.qualifier.NVarchar;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.core.qualifier.Qualifier;
import org.jdbi.core.qualifier.Qualifiers;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Measurement(time = 5)
@Warmup(time = 2)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(4)
public class QualifiersBenchmark {

    private Jdbi jdbi;
    private Qualifiers qualifiers;

    @Setup
    public void setup() throws Throwable {
        jdbi = Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID());
        jdbi.registerRowMapper(BeanMapper.factory(UnqualifiedBean.class));
        jdbi.registerRowMapper(BeanMapper.factory(QualifiedBean1.class));
        jdbi.registerRowMapper(BeanMapper.factory(QualifiedBean2.class));
        jdbi.registerRowMapper(BeanMapper.factory(QualifiedBean3.class));
        jdbi.registerRowMapper(BeanMapper.factory(QualifiedBean4.class));
        qualifiers = new Qualifiers();
        qualifiers.setRegistry(new ConfigRegistry());
    }

    @Benchmark
    public QualifiedType<?> qualifiedType0() {
        return qualifiers.qualifiedTypeOf(UnqualifiedBean.class);
    }

    @Benchmark
    public QualifiedType<?> qualifiedType1() {
        return qualifiers.qualifiedTypeOf(QualifiedBean1.class);
    }

    @Benchmark
    public QualifiedType<?> qualifiedType2() {
        return qualifiers.qualifiedTypeOf(QualifiedBean2.class);
    }

    @Benchmark
    public QualifiedType<?> qualifiedType3() {
        return qualifiers.qualifiedTypeOf(QualifiedBean3.class);
    }

    @Benchmark
    public QualifiedType<?> qualifiedType4() {
        return qualifiers.qualifiedTypeOf(QualifiedBean4.class);
    }

    @Benchmark
    public boolean eq0To0() {
        return qualifiers.qualifiedTypeOf(UnqualifiedBean.class).getQualifiers()
                .equals(qualifiers.findFor(UnqualifiedBean.class));
    }

    @Benchmark
    public boolean neq1To0() {
        return qualifiers.qualifiedTypeOf(QualifiedBean1.class).getQualifiers()
                .equals(qualifiers.findFor(UnqualifiedBean.class));
    }

    @Benchmark
    public boolean eq1To1() {
        return qualifiers.qualifiedTypeOf(QualifiedBean1.class).getQualifiers()
                .equals(qualifiers.findFor(QualifiedBean1.class));
    }

    @Benchmark
    public boolean neq1To1() {
        return qualifiers.qualifiedTypeOf(QualifiedBean1.class).getQualifiers()
                .equals(qualifiers.findFor(QualifiedBean1B.class));
    }

    @Benchmark
    public boolean eq2To2() {
        return qualifiers.qualifiedTypeOf(QualifiedBean2.class).getQualifiers()
                .equals(qualifiers.findFor(QualifiedBean2.class));
    }

    @Benchmark
    public boolean neq2To2() {
        return qualifiers.qualifiedTypeOf(QualifiedBean2.class).getQualifiers()
                .equals(qualifiers.findFor(QualifiedBean2B.class));
    }

    @Benchmark
    public boolean eq3To3() {
        return qualifiers.qualifiedTypeOf(QualifiedBean3.class).getQualifiers()
                .equals(qualifiers.findFor(QualifiedBean3.class));
    }

    @Benchmark
    public boolean neq3To3() {
        return qualifiers.qualifiedTypeOf(QualifiedBean3.class).getQualifiers()
                .equals(qualifiers.findFor(QualifiedBean3B.class));
    }

    @Benchmark
    public boolean eq4To4() {
        return qualifiers.qualifiedTypeOf(QualifiedBean4.class).getQualifiers()
                .equals(qualifiers.findFor(QualifiedBean4.class));
    }

    @Benchmark
    public boolean neq4To4() {
        return qualifiers.qualifiedTypeOf(QualifiedBean4.class).getQualifiers()
                .equals(qualifiers.findFor(QualifiedBean4B.class));
    }

    @Benchmark
    public UnqualifiedBean mapUnqualifiedBean() {
        return jdbi.withHandle(h -> h.createQuery("select 'a' as a, 'b' as b, 'c' as c").mapTo(UnqualifiedBean.class).one());
    }

    @Benchmark
    public QualifiedBean1 mapQualifiedBean() {
        return jdbi.withHandle(h -> h.createQuery("select 'a' as a, 'b' as b, 'c' as c").mapTo(QualifiedBean1.class).one());
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
    public static class QualifiedBean1 extends BaseBean {}

    @Q2(false)
    public static class QualifiedBean1B extends BaseBean {}

    @NVarchar
    @Q1
    public static class QualifiedBean2 extends BaseBean {}

    @NVarchar
    @Q2(false)
    public static class QualifiedBean2B extends BaseBean {}

    @NVarchar
    @Q1
    @Q2(true)
    public static class QualifiedBean3 extends BaseBean {}

    @NVarchar
    @Q1
    @Q3
    public static class QualifiedBean3B extends BaseBean {}

    @NVarchar
    @Q1
    @Q2(true)
    @Q3
    public static class QualifiedBean4 extends BaseBean {}

    @NVarchar
    @Q1
    @Q2(false)
    @Q3
    public static class QualifiedBean4B extends BaseBean {}

    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    public @interface Q1 {}

    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    public @interface Q2 {
        boolean value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    public @interface Q3 {}
}
