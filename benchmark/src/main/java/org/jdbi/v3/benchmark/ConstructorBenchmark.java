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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.TimeUnit;

import org.jdbi.v3.core.internal.JdbiClassUtils;
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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@Measurement(time = 5)
@Warmup(time = 2)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(4)
public class ConstructorBenchmark {

    private static final MethodHandle CTOR;
    private static final MethodHandle CTOR_OBJ;

    MethodHandle nonfinalCtor;

    static {
        try {
            CTOR = MethodHandles.lookup()
                    .findConstructor(Instantiated.class, MethodType.methodType(void.class));
            CTOR_OBJ = CTOR.asType(MethodType.methodType(Object.class));
        } catch (final ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void main(final String[] args) throws RunnerException {
        final Options options = new OptionsBuilder()
            .include(BeanBindingBenchmark.class.getSimpleName())
            .forks(0)
            .build();
        new Runner(options).run();
    }

    @Setup
    public void setup() {
        nonfinalCtor = CTOR;
    }

    @Benchmark
    public Instantiated constructorNonfinalMhInvokeExact() throws Throwable {
        return (Instantiated) nonfinalCtor.invokeExact();
    }

    @Benchmark
    public Object constructorMhInvokeExactAsType() throws Throwable {
        return CTOR_OBJ.invokeExact();
    }

    @Benchmark
    public Instantiated constructorMhInvokeExact() throws Throwable {
        return (Instantiated) CTOR.invokeExact();
    }

    @Benchmark
    public Object constructorMhInvoke() throws Throwable {
        return CTOR.invoke();
    }

    @Benchmark
    public Object constructorMhInvokeArgs() throws Throwable {
        return CTOR.invokeWithArguments();
    }

    @Benchmark
    public Instantiated jcuCheckedCreate() {
        return JdbiClassUtils.checkedCreateInstance(Instantiated.class);
    }

    @Benchmark
    public Instantiated jcuFindInstantiate() {
        return JdbiClassUtils.findConstructorAndCreateInstance(Instantiated.class, new Class<?>[0],
                handle -> handle.invokeExact());
    }

    public static class Instantiated {
    }
}
