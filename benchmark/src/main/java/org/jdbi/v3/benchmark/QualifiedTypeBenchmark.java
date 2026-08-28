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

import org.jdbi.v3.core.enums.EnumByName;
import org.jdbi.v3.core.qualifier.NVarchar;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Measurement(time = 3)
@Warmup(time = 2)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(2)
public class QualifiedTypeBenchmark {
    @NVarchar
    private static final class Holder {}

    private final QualifiedType<String> plain = QualifiedType.of(String.class);
    private final QualifiedType<String> nvarchar = QualifiedType.of(String.class).with(NVarchar.class);
    private final Set<Annotation> none = Set.of();
    private final Set<Annotation> realNvarchar = Set.of(Holder.class.getAnnotation(NVarchar.class));

    @Benchmark
    public boolean hasQualifiersEmptyMatch() {
        return plain.hasQualifiers(none);
    }

    @Benchmark
    public boolean hasQualifiersOneMatch() {
        return nvarchar.hasQualifiers(realNvarchar);
    }

    @Benchmark
    public boolean hasQualifiersSizeMismatch() {
        return nvarchar.hasQualifiers(none);
    }

    @Benchmark
    public boolean hasQualifier() {
        return nvarchar.hasQualifier(EnumByName.class);
    }

    @Benchmark
    public QualifiedType<String> withClass() {
        return QualifiedType.of(String.class).with(NVarchar.class);
    }

    @Benchmark
    public QualifiedType<String> withAnnotations() {
        return QualifiedType.of(String.class).withAnnotations(realNvarchar);
    }
}
