/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jdbi.v3.core.qualifier;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jdbi.v3.core.Jdbi;
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

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Measurement(time=5)
@Warmup(time=2)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
public class QualifiersBenchmark {

    JdbiRule db;
    Jdbi jdbi;

    @Setup
    public void setup() throws Throwable {
        db = JdbiRule.h2();
        db.before();
        jdbi = db.getJdbi();
        jdbi.registerRowMapper(BeanMapper.factory(UnqualifiedBean.class));
        jdbi.registerRowMapper(BeanMapper.factory(QualifiedBean.class));
    }

    @TearDown
    public void close() {
        db.after();
    }

    @Benchmark
    public Set<Annotation> getQualifiersUnannotated() {
        return Qualifiers.getQualifiers(UnqualifiedBean.class);
    }

    @Benchmark
    public Set<Annotation> getQualifiersAnnotated() {
        return Qualifiers.getQualifiers(QualifiedBean.class);
    }

    @Benchmark
    public UnqualifiedBean mapUnqualifiedBean() {
        return jdbi.withHandle(h -> h.createQuery("select 'a' as a, 'b' as b, 'c' as c").mapTo(UnqualifiedBean.class).findOnly());
    }

    @Benchmark
    public QualifiedBean mapQualifiedBean() {
        return jdbi.withHandle(h -> h.createQuery("select 'a' as a, 'b' as b, 'c' as c").mapTo(QualifiedBean.class).findOnly());
    }

    public static class BaseBean {
        private String a, b, c;

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
