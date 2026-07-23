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
package org.jdbi.v3.core.cache.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import org.jdbi.v3.core.cache.JdbiCache;
import org.jdbi.v3.core.cache.JdbiCacheLoader;
import org.jdbi.v3.core.internal.exceptions.Sneaky;

final class DefaultJdbiCache<K, V> implements JdbiCache<K, V> {

    private final ConcurrentMap<K, DoubleLinkedList.Node<K, CompletableFuture<V>>> cache;

    @GuardedBy("expungeQueue")
    private final DoubleLinkedList<K, CompletableFuture<V>> expungeQueue;

    private final JdbiCacheLoader<K, V> cacheLoader;

    private final int maxSize;

    DefaultJdbiCache(DefaultJdbiCacheBuilder builder, JdbiCacheLoader<K, V> cacheLoader) {
        this.cache = new ConcurrentHashMap<>();
        this.expungeQueue = new DoubleLinkedList<>();
        this.cacheLoader = cacheLoader;

        this.maxSize = builder.getMaxSize();
    }

    @Override
    public V get(K key) {
        try {
            return doGet(key, cacheLoader);
        } finally {
            expunge();
        }
    }

    @Override
    public V getWithLoader(K key, JdbiCacheLoader<K, V> loader) {
        try {
            return doGet(key, loader);
        } finally {
            expunge();
        }
    }

    private V doGet(final K key, final JdbiCacheLoader<K, V> loader) {
        var node = cache.computeIfAbsent(key, k ->
                DoubleLinkedList.createNode(key, new CompletableFuture<>()));
        if (node.value.isDone()) {
            if (!node.value.isCompletedExceptionally()) {
                refresh(node);
            }
            return join(node);
        }

        // Node with Future atomically loaded into cache, but needs a value still
        // CHM and friends use a striped lock which can lead to surprising exclusions
        // https://github.com/jdbi/jdbi/issues/2834
        // so take a more specific lock instead
        synchronized (node) {
            // Double-check in case of race
            if (!node.value.isDone()) {
                final V value;
                try {
                    value = loader == null ? null : loader.create(key);
                } catch (Throwable e) {
                    // The loader failed. Complete the placeholder exceptionally so a racing caller that
                    // already holds this node observes the same failure instead of re-running the loader,
                    // then drop the node so a later call retries with a fresh one. Without this the
                    // permanently-incomplete node would be found again and re-added to the expunge queue
                    // (which would throw "Can not add node twice!"). The node was never added to the
                    // expunge queue, since the value is computed first.
                    node.value.completeExceptionally(e);
                    cache.remove(key, node);
                    throw e;
                }
                if (maxSize > 0) {
                    synchronized (expungeQueue) {
                        expungeQueue.addHead(node);
                    }
                }
                node.value.complete(value);
            }
            return join(node);
        }
    }

    /**
     * Join on a node's value, discarding the {@link CompletionException} wrapper that
     * {@link CompletableFuture#join()} adds around a loader failure. Every caller then sees the same
     * exception the loader threw, regardless of which thread won the race to compute the key.
     */
    // PreserveStackTrace: the wrapper carries only CompletableFuture plumbing; the rethrown cause keeps its own stack.
    @SuppressWarnings("PMD.PreserveStackTrace")
    private V join(DoubleLinkedList.Node<K, CompletableFuture<V>> node) {
        try {
            return node.value.join();
        } catch (CompletionException e) {
            throw Sneaky.throwAnyway(e.getCause());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public DefaultJdbiCacheStats getStats() {
        synchronized (expungeQueue) {
            return new DefaultJdbiCacheStats(expungeQueue.size, maxSize);
        }
    }

    private void refresh(DoubleLinkedList.Node<K, CompletableFuture<V>> node) {
        if (maxSize > 0) {
            synchronized (expungeQueue) {
                DoubleLinkedList.Node<K, CompletableFuture<V>> cacheNode = expungeQueue.removeNode(node);
                // this can happen if the node that should be refreshed has been
                // expunged between the call to computeIfAbsent and refresh (which is not
                // done under the lock) above, so by the time the node gets passed here
                // it is no longer in the list.
                if (cacheNode != null) {
                    expungeQueue.addHead(cacheNode);
                }
            }
        }
    }

    private void expunge() {
        // If the code would not use a purge list, it would acquire
        // first the expunge lock and then the stripe lock.
        //
        // however any call to the wrapLoader method above happens
        // while the stripe lock is held and then acquires the
        // expunge lock afterwards. ( stripe lock -> expunge lock)
        //
        // these two paths combined then create a lock inversion and
        // a thread deadlock (#2274, only 3.37.0 is affected).
        //
        // the purge list is filled within the expunge lock but does
        // not touch the cache. The cache is then purged outside the lock
        // so the stripe lock is not nested within the expunge lock.
        //
        // multithreading is hard.
        final List<K> purgeList = new ArrayList<>();

        synchronized (expungeQueue) {
            if (maxSize <= 0 || expungeQueue.size <= maxSize) {
                // unbounded or not yet full
                return;
            }

            while (expungeQueue.size > maxSize) {
                DoubleLinkedList.Node<K, ?> node = expungeQueue.removeTail();
                if (node != null) {
                    purgeList.add(node.key);
                }
            }
        }

        purgeList.forEach(cache::remove);
    }
}
