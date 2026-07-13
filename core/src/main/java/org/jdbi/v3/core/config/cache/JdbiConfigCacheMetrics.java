package org.jdbi.v3.core.config.cache;

import java.util.concurrent.atomic.AtomicInteger;

class JdbiConfigCacheMetrics {
    private final AtomicInteger cacheHits = new AtomicInteger();
    private final AtomicInteger cacheMisses = new AtomicInteger();
    private final AtomicInteger cacheWrites = new AtomicInteger();

    JdbiConfigCacheMetrics() {
    }

    void hit() {
        cacheHits.incrementAndGet();
    }

    void miss() {
        cacheMisses.incrementAndGet();
    }

    void put() {
        cacheWrites.incrementAndGet();
    }

    int getCacheWrites() {
        return cacheWrites.incrementAndGet();
    }

    int getCacheHits() {
        return cacheHits.get();
    }

    int getCacheMisses() {
        return cacheMisses.get();
    }
}
