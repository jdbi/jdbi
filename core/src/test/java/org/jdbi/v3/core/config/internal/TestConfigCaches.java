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
package org.jdbi.v3.core.config.internal;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class TestConfigCaches {

    // Every test runs under a hard timeout: a regression in the cache's locking discipline shows up as a
    // failing (timed-out) test, never as a hung build. See https://github.com/jdbi/jdbi/issues/2980.
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Test
    void computesValueOnce() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final AtomicInteger computations = new AtomicInteger();
            final ConfigCache<String, String> cache =
                ConfigCaches.declare(key -> {
                    computations.incrementAndGet();
                    return key.toUpperCase();
                });
            final ConfigRegistry config = new ConfigRegistry();

            assertThat(cache.get("a", config)).isEqualTo("A");
            assertThat(cache.get("a", config)).isEqualTo("A");

            assertThat(computations).hasValue(1);
        });
    }

    @Test
    void rejectsNullValue() {
        // A null result is rejected, and like any failed computation it is not memoized, so a later
        // (non-null) call still succeeds.
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final AtomicInteger computations = new AtomicInteger();
            final ConfigCache<String, String> cache =
                ConfigCaches.declare(key -> computations.incrementAndGet() == 1 ? null : key.toUpperCase());
            final ConfigRegistry config = new ConfigRegistry();

            assertThatThrownBy(() -> cache.get("a", config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("null");

            assertThat(cache.get("a", config)).isEqualTo("A");
            assertThat(computations).hasValue(2);
        });
    }

    @Test
    void doesNotCacheComputationFailure() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final AtomicInteger attempts = new AtomicInteger();
            final ConfigCache<String, String> cache =
                ConfigCaches.declare(key -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw new IllegalStateException("boom");
                    }
                    return key.toUpperCase();
                });
            final ConfigRegistry config = new ConfigRegistry();

            assertThatThrownBy(() -> cache.get("a", config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

            // A failure is not memoized: a subsequent call retries and succeeds.
            assertThat(cache.get("a", config)).isEqualTo("A");
            assertThat(attempts).hasValue(2);
        });
    }

    @Test
    void reentrantDifferentKeyComputationSucceeds() {
        // A computer recursing into the same cache for a different key (the mapper-warming pattern from
        // #2980) must not throw "Recursive update" or deadlock.
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final ConfigRegistry config = new ConfigRegistry();

            @SuppressWarnings("unchecked")
            final ConfigCache<Integer, Integer>[] holder = new ConfigCache[1];
            // fib(n) recurses into fib(n-1) and fib(n-2) through the same cache. Many distinct keys are
            // populated reentrantly, so at least one pair collides into a shared bin on any realistic table.
            holder[0] = ConfigCaches.declare(n ->
                n < 2 ? n : holder[0].get(n - 1, config) + holder[0].get(n - 2, config));

            assertThat(holder[0].get(30, config)).isEqualTo(832040);
        });
    }

    @Test
    void concurrentReentrantComputationDoesNotDeadlock() {
        // Many threads warming overlapping reentrant entries from distinct starting keys, so they acquire
        // bin monitors in opposite orders — the ABBA hazard from #2980.
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final ConfigRegistry config = new ConfigRegistry();

            @SuppressWarnings("unchecked")
            final ConfigCache<Integer, Integer>[] holder = new ConfigCache[1];
            final CountDownLatch start = new CountDownLatch(1);
            holder[0] = ConfigCaches.declare(n ->
                n < 2 ? n : holder[0].get(n - 1, config) + holder[0].get(n - 2, config));

            final int threads = 8;
            final ExecutorService pool = Executors.newFixedThreadPool(threads);
            try {
                final List<Future<Integer>> results = IntStream.range(0, threads)
                    .mapToObj(i -> pool.submit(() -> {
                        await(start);
                        return holder[0].get(25 + i, config);
                    }))
                    .collect(toList());

                start.countDown();
                for (Future<Integer> result : results) {
                    assertThat(result.get()).isPositive();
                }
            } finally {
                pool.shutdownNow();
            }
        });
    }

    @Test
    void concurrentGetsComputeOnce() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final int threads = 16;
            final AtomicInteger computations = new AtomicInteger();
            final CountDownLatch start = new CountDownLatch(1);
            final ConfigCache<String, String> cache =
                ConfigCaches.declare(key -> {
                    computations.incrementAndGet();
                    return key.toUpperCase();
                });
            final ConfigRegistry config = new ConfigRegistry();

            final ExecutorService pool = Executors.newFixedThreadPool(threads);
            try {
                final List<Future<String>> results = IntStream.range(0, threads)
                    .mapToObj(i -> pool.submit(() -> {
                        await(start);
                        return cache.get("key", config);
                    }))
                    .collect(toList());

                start.countDown();
                for (Future<String> result : results) {
                    assertThat(result.get()).isEqualTo("KEY");
                }
            } finally {
                pool.shutdownNow();
            }

            assertThat(computations).hasValue(1);
        });
    }

    @Test
    void selfRecursiveComputationFailsFast() {
        // A computer that re-enters the same cache for the key it is computing describes a value that
        // depends on itself. We reject it rather than self-deadlock on the join, mirroring
        // ConcurrentHashMap.computeIfAbsent's behavior for recursive same-key updates.
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final ConfigRegistry config = new ConfigRegistry();
            @SuppressWarnings("rawtypes")
            final ConfigCache[] holder = new ConfigCache[1];
            final ConfigCache<String, String> cache =
                ConfigCaches.declare(key -> (String) holder[0].get(key, config));
            holder[0] = cache;

            assertThatThrownBy(() -> cache.get("a", config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Recursive");
        });
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted", e);
        }
    }
}
