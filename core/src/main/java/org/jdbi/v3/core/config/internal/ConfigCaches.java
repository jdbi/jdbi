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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.internal.exceptions.Sneaky;

/**
 * Hold metadata caches which maps various JVM constants into pre-parsed forms.
 * For example, bean property accessors, or normalized enum constants.
 * Note that unlike most JdbiConfig types, this cache is Jdbi level and shared,
 * so it should not hold data that needs to respect reconfiguration.
 * Additionally, currently there is no expiration policy, as nearly all keys are
 * references to JVM constant pool entries.
 * <b>This makes it unsuitable as a general-purpose shared cache.</b>
 */
public final class ConfigCaches implements JdbiConfig<ConfigCaches> {

    private final Map<ConfigCache<?, ?>, Map<Object, Object>> caches = new ConcurrentHashMap<>();

    /**
     * Does not actually create a copy!!
     */
    @Override
    public ConfigCaches createCopy() {
        return this;
    }

    public static <K, V> ConfigCache<K, V> declare(Function<K, V> computer) {
        return declare(Function.identity(), computer);
    }

    public static <K, V> ConfigCache<K, V> declare(Function<K, ?> keyNormalizer, Function<K, V> computer) {
        return declare(keyNormalizer, (config, k) -> computer.apply(k));
    }

    public static <K, V> ConfigCache<K, V> declare(BiFunction<ConfigRegistry, K, V> computer) {
        return declare(Function.identity(), computer);
    }

    public static <K, V> ConfigCache<K, V> declare(Function<K, ?> keyNormalizer, BiFunction<ConfigRegistry, K, V> computer) {
        return new ConfigCache<>() {
            // PreserveStackTrace: the join() path deliberately discards the CompletionException wrapper and
            // rethrows its cause; the wrapper carries only CompletableFuture plumbing, and the cause keeps
            // its own stack. See the comment at the throw site.
            @SuppressWarnings({"unchecked", "PMD.PreserveStackTrace"})
            @Override
            public V get(K key, ConfigRegistry config) {
                // The per-cache map allocation below runs a trivial, non-reentrant mapping function, so
                // holding the bin monitor for it is harmless.
                final Map<Object, Object> cache = config.get(ConfigCaches.class).caches
                    .computeIfAbsent(this, x -> new ConcurrentHashMap<>());

                final Object normalizedKey = keyNormalizer.apply(key);

                // Deliberately not computeIfAbsent: that holds the ConcurrentHashMap bin monitor for the
                // duration of the mapping function, and our computers routinely recurse into other caches
                // (and other bins of this same map) to resolve qualifiers, column mappers, and the like.
                // Two threads warming such mappers concurrently can take bin monitors in opposite order and
                // deadlock (ABBA). See https://github.com/jdbi/jdbi/issues/2980.
                //
                // Instead we install an incomplete entry under the key with an atomic putIfAbsent (whose
                // bin monitor is never held across user code), then run the computer with no lock held. The
                // thread that wins the race computes exactly once; everyone else joins its result.
                final CacheEntry entry = new CacheEntry();
                final CacheEntry existing = (CacheEntry) cache.putIfAbsent(normalizedKey, entry);
                if (existing != null) {
                    if (Thread.currentThread().equals(existing.owner) && !existing.value.isDone()) {
                        // A computer on this thread re-entered the same cache for the same key it is still
                        // computing. The value depends on itself; no caching strategy can satisfy it. Fail
                        // fast with a useful message instead of self-deadlocking on the join below. This
                        // mirrors ConcurrentHashMap.computeIfAbsent, which throws on recursive same-key updates.
                        throw new IllegalStateException("Recursive cache computation for key " + key);
                    }
                    // The winning thread has already run the computer and completed (or failed) the future,
                    // so join() does not block on user code while holding any lock. On failure, unwrap the
                    // CompletionException so a thread that lost the race sees the same exception the computing
                    // thread would throw: the type a caller catches must not depend on who won the race.
                    try {
                        return (V) existing.value.join();
                    } catch (CompletionException e) {
                        throw Sneaky.throwAnyway(e.getCause());
                    }
                }

                final Object computed;
                try {
                    computed = computer.apply(config, key);
                } catch (RuntimeException | Error e) {
                    fail(cache, normalizedKey, entry, e);
                    throw e;
                }
                if (computed == null) {
                    // These caches map JVM constants to a derived form; "no value" is not a meaningful
                    // result. Reject null rather than cache it and risk an NPE far downstream. A computer
                    // that needs to express absence should return an Optional or a sentinel, not null.
                    final IllegalStateException e = new IllegalStateException("Cache computer returned null for key " + key);
                    fail(cache, normalizedKey, entry, e);
                    throw e;
                }
                entry.value.complete(computed);
                return (V) computed;
            }
        };
    }

    /**
     * Abandon a computation that did not produce a usable value: drop the entry so a later call retries
     * (we deliberately do not memoize failures), and hand the cause to any threads already joining the key.
     */
    private static void fail(Map<Object, Object> cache, Object normalizedKey, CacheEntry entry, Throwable cause) {
        cache.remove(normalizedKey, entry);
        entry.value.completeExceptionally(cause);
    }

    /**
     * A cache slot. {@code value} is completed exactly once by the thread that installed the entry;
     * {@code owner} is that thread, retained only to detect a computer re-entering the cache for the key
     * it is itself computing (an unsatisfiable self-dependency we must reject rather than deadlock on).
     */
    private static final class CacheEntry {
        final Thread owner = Thread.currentThread();
        final CompletableFuture<Object> value = new CompletableFuture<>();
    }
}
