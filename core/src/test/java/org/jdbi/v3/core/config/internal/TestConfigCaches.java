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
import java.util.concurrent.CompletableFuture;
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
    void concurrentLoserSeesOriginalException() {
        // A thread that lost the race and joined the in-flight entry must see the computer's original
        // exception type, not a CompletionException wrapping it. (E.g. EnumMapper's computer throws
        // UnableToProduceResultException for an unmatched name; a concurrent caller must still see that.)
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final ConfigRegistry config = new ConfigRegistry();
            final CountDownLatch computing = new CountDownLatch(1);
            final CountDownLatch loserArrived = new CountDownLatch(1);

            final ConfigCache<String, String> cache =
                ConfigCaches.declare(key -> {
                    // Announce that this thread owns the in-flight entry, then wait until the loser is stalled
                    // on the entry before failing. This makes the loser deterministically observe a failure
                    // produced by another thread rather than computing the value itself.
                    computing.countDown();
                    await(loserArrived);
                    throw new CustomException("boom");
                });

            final ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                final Future<String> winner = pool.submit(() -> cache.get("a", config));
                await(computing);

                // Capture the loser's thread so we can observe when it stalls inside get() waiting on the
                // winner, instead of guessing with a fixed sleep.
                final CompletableFuture<Thread> loserThread = new CompletableFuture<>();
                final Future<String> loser = pool.submit(() -> {
                    loserThread.complete(Thread.currentThread());
                    return cache.get("a", config);
                });

                // Wait until the loser is stalled inside get() waiting on the winner, then release the winner
                // to fail. This guarantees the loser is committed to this computation rather than racing to a
                // fresh one, so its observed exception reflects the shared failure.
                final Thread t = loserThread.join();
                awaitThreadStalled(t);
                loserArrived.countDown();

                assertThatThrownBy(winner::get)
                    .hasCauseInstanceOf(CustomException.class);
                assertThatThrownBy(loser::get)
                    .hasCauseInstanceOf(CustomException.class);
            } finally {
                pool.shutdownNow();
            }
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

    private static final class CustomException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        CustomException(String message) {
            super(message);
        }
    }

    // Spin until the thread is stalled inside get() waiting on the winner — parked on the future (WAITING)
    // or blocked on a bin monitor (BLOCKED). Either way it is committed to the lookup and cannot recompute
    // before the winner is released, which makes the race outcome deterministic; accepting both states keeps
    // the handshake independent of get()'s locking strategy. The enclosing timeout bounds the spin.
    private static void awaitThreadStalled(Thread thread) {
        while (true) {
            final Thread.State state = thread.getState();
            if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING || state == Thread.State.BLOCKED) {
                return;
            }
            if (state == Thread.State.TERMINATED) {
                throw new IllegalStateException("thread terminated before stalling on the cache entry");
            }
            Thread.onSpinWait();
        }
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
