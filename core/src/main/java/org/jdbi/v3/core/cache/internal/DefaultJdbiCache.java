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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import org.jdbi.v3.core.cache.JdbiCache;
import org.jdbi.v3.core.cache.JdbiCacheLoader;

final class DefaultJdbiCache<K, V> implements JdbiCache<K, V> {

    private final ConcurrentMap<K, DoubleLinkedList.Node<K, V>> cache;

    @GuardedBy("expungeQueue")
    private final DoubleLinkedList<K, V> expungeQueue;

    private final JdbiCacheLoader<K, DoubleLinkedList.Node<K, V>> cacheLoader;

    private final int maxSize;

    DefaultJdbiCache(DefaultJdbiCacheBuilder builder, JdbiCacheLoader<K, V> cacheLoader) {
        this.cache = new ConcurrentHashMap<>();
        this.expungeQueue = new DoubleLinkedList<>();
        this.cacheLoader = wrapLoader(cacheLoader);

        this.maxSize = builder.getMaxSize();
    }

    @Override
    public V get(K key) {
        try {
            DoubleLinkedList.Node<K, V> node = cache.computeIfAbsent(key, cacheLoader::create);
            refresh(node);
            return node.value;
        } finally {
            expunge();
        }
    }

    @Override
    public V getWithLoader(K key, JdbiCacheLoader<K, V> loader) {
        try {
            DoubleLinkedList.Node<K, V> node = cache.computeIfAbsent(key, wrapLoader(loader)::create);
            refresh(node);
            return node.value;
        } finally {
            expunge();
        }
    }

    @Override
    public DefaultJdbiCacheStats getStats() {
        synchronized (expungeQueue) {
            return new DefaultJdbiCacheStats(expungeQueue.size, maxSize);
        }
    }

    private JdbiCacheLoader<K, DoubleLinkedList.Node<K, V>> wrapLoader(JdbiCacheLoader<K, V> delegate) {

        if (delegate == null) {
            return k -> DoubleLinkedList.createNode(k, null);
        }

        return key -> {
            V value = delegate.create(key);
            DoubleLinkedList.Node<K, V> node = DoubleLinkedList.createNode(key, value);
            if (maxSize > 0) {
                synchronized (expungeQueue) {
                    expungeQueue.addHead(node);
                }
            }
            return node;
        };
    }

    private void refresh(DoubleLinkedList.Node<K, V> node) {
        if (maxSize > 0) {
            synchronized (expungeQueue) {
                DoubleLinkedList.Node<K, V> cacheNode = expungeQueue.removeNode(node);
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
                DoubleLinkedList.Node<K, V> node = expungeQueue.removeTail();
                if (node != null) {
                    purgeList.add(node.key);
                }
            }
        }

        purgeList.forEach(cache::remove);
    }
}
