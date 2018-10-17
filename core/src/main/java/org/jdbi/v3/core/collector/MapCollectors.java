package org.jdbi.v3.core.collector;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class MapCollectors {
    private MapCollectors() {
        throw new UnsupportedOperationException("utility class");
    }

    public static <K, V, M extends Map<K, V>> Collector<Map.Entry<K, V>, ?, M> toMap(Supplier<M> mapFactory) {
        return Collector.of(
            mapFactory,
            MapCollectors::putEntry,
            MapCollectors::combine);
    }

    private static <K, V, M extends Map<K, V>> void putEntry(M map, Map.Entry<K, V> entry) {
        putEntry(map, entry.getKey(), entry.getValue());
    }

    private static <K, V, M extends Map<K, V>> void putEntry(M map, K key, V value) {
        V oldValue = map.put(key, value);
        if (oldValue != null) {
            throw new IllegalStateException(String.format(
                "Multiple values for Map key '%s': ['%s','%s',...]",
                key,
                oldValue,
                value));
        }
    }

    private static <K, V, M extends Map<K, V>> M combine(M a, M b) {
        b.forEach((k, v) -> putEntry(a, k, v));
        return a;
    }
}
