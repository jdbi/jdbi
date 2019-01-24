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

import java.text.Collator;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Measurement(time = 5)
@Warmup(time = 2)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
public class CaseInsensitiveStringEqualsBenchmark {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
            .include(CaseInsensitiveStringEqualsBenchmark.class.getSimpleName())
            .forks(1)
            .build();
        new Runner(options).run();
    }

    private Random random;
    private String original;
    private String copy;
    private String randomizedCase;
    private String other;

    private Collator collator;

    @Setup
    public void setup() {
        random = new Random();
        original = randomAlphabetic(10);
        copy = new String(original);
        randomizedCase = randomizeCase(original);
        other = randomAlphabetic(10);

        collator = Collator.getInstance(Locale.ROOT);
        collator.setStrength(Collator.SECONDARY);
    }

    @Benchmark
    public boolean stringEquals() {
        return original.equals(copy);
    }

    @Benchmark
    public boolean stringNotEquals() {
        return original.equals(other);
    }

    @Benchmark
    public boolean toLowerCaseEqualsCopy() {
        return original.toLowerCase().equalsIgnoreCase(copy);
    }

    @Benchmark
    public boolean toLowerCaseEqualsRandomCase() {
        return original.toLowerCase().equalsIgnoreCase(copy);
    }

    @Benchmark
    public boolean toLowerCaseEqualsOther() {
        return original.toLowerCase().equalsIgnoreCase(other);
    }

    @Benchmark
    public boolean equalsIgnoreCaseCopy() {
        return original.equalsIgnoreCase(copy);
    }

    @Benchmark
    public boolean equalsIgnoreCaseRandomCase() {
        return original.equalsIgnoreCase(randomizedCase);
    }

    @Benchmark
    public boolean equalsIgnoreCaseOther() {
        return original.equalsIgnoreCase(other);
    }

    @Benchmark
    public boolean collatorEqualsCopy() {
        return collator.equals(original, copy);
    }

    @Benchmark
    public boolean collatorEqualsRandomCase() {
        return collator.equals(original, randomizedCase);
    }

    @Benchmark
    public boolean collatorEqualsOther() {
        return collator.equals(original, other);
    }

    private String randomAlphabetic(int length) {
        StringBuilder b = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            b.append(randomAlphabetic());
        }

        return b.toString();
    }

    private char randomAlphabetic() {
        return ALPHABET.charAt(random.nextInt(ALPHABET.length()));
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
