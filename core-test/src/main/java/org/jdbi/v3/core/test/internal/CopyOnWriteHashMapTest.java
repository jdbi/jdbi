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
package org.jdbi.v3.core.test.internal;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.jdbi.v3.core.internal.CopyOnWriteHashMap;
import org.junit.jupiter.api.Test;

public class CopyOnWriteHashMapTest {
    @Test
    void put() {
        Map<String, String> a = new CopyOnWriteHashMap<>();
        assertThat(a.isEmpty()).isTrue();
        assertThat(a.size()).isEqualTo(0);
        assertThat(a.put("a", "1")).isNull();
        assertThat(a.isEmpty()).isFalse();
        assertThat(a.size()).isEqualTo(1);
        Map<String, String> b = new CopyOnWriteHashMap<>(a);
        assertThat(b.get("a")).isEqualTo("1");
        assertThat(a.put("a", "2")).isEqualTo("1");
        assertThat(a.get("a")).isEqualTo("2");
        assertThat(b.get("a")).isEqualTo("1");
        assertThat(b.put("a", "3")).isEqualTo("1");
        assertThat(a.get("a")).isEqualTo("2");
        assertThat(b.get("a")).isEqualTo("3");
    }

    @Test
    void putIfAbsent() {
        Map<String, String> a = new CopyOnWriteHashMap<>();
        assertThat(a.putIfAbsent("a", "1")).isNull();
        assertThat(a.putIfAbsent("a", "2")).isEqualTo("1");
        assertThat(a.get("a")).isEqualTo("1");
    }

    @Test
    void putAll() {
        Map<String, String> a = new CopyOnWriteHashMap<>();
        a.putAll(singletonMap("a", "1"));
        Map<String, String> b = new CopyOnWriteHashMap<>(a);
        assertThat(b.get("a")).isEqualTo("1");
        a.putAll(singletonMap("a", "2"));
        assertThat(a.get("a")).isEqualTo("2");
        assertThat(b.get("a")).isEqualTo("1");
        b.putAll(singletonMap("a", "3"));
        assertThat(a.get("a")).isEqualTo("2");
        assertThat(b.get("a")).isEqualTo("3");
    }

    @Test
    void remove() {
        Map<String, String> a = new CopyOnWriteHashMap<>();
        assertThat(a.remove("a")).isNull();
        a.put("a", "1");
        Map<String, String> b = new CopyOnWriteHashMap<>(a);
        assertThat(b.get("a")).isEqualTo("1");
        assertThat(a.remove("a")).isEqualTo("1");
        assertThat(a.get("a")).isNull();
        assertThat(b.get("a")).isEqualTo("1");
        b.put("a", "3");
        assertThat(a.get("a")).isNull();
        assertThat(b.get("a")).isEqualTo("3");
    }

    @Test
    void clear() {
        Map<String, String> a = new CopyOnWriteHashMap<>();
        a.put("a", "1");
        Map<String, String> b = new CopyOnWriteHashMap<>(a);
        assertThat(b.get("a")).isEqualTo("1");
        a.clear();
        assertThat(a.get("a")).isNull();
        assertThat(b.get("a")).isEqualTo("1");
        b.put("a", "3");
        assertThat(a.get("a")).isNull();
        assertThat(b.get("a")).isEqualTo("3");
    }

    @Test
    void compute() {
        Map<String, String> a = new CopyOnWriteHashMap<>();
        assertThat(a.compute("a", (k, v) -> {
            assertThat(k).isEqualTo("a");
            assertThat(v).isNull();
            return "1";
        })).isEqualTo("1");
        assertThat(a.compute("a", (k, v) -> {
            assertThat(k).isEqualTo("a");
            assertThat(v).isEqualTo("1");
            return "2";
        })).isEqualTo("2");
        assertThat(a.get("a")).isEqualTo("2");
    }

    @Test
    void computeIfAbsent() {
        Map<String, String> a = new CopyOnWriteHashMap<>();
        assertThat(a.computeIfAbsent("a", k -> {
            assertThat(k).isEqualTo("a");
            return "1";
        })).isEqualTo("1");
        assertThat(a.computeIfAbsent("a", k -> {
            throw new AssertionError();
        })).isEqualTo("1");
        assertThat(a.get("a")).isEqualTo("1");
    }

    @Test
    void computeIfPresent() {
        Map<String, String> a = new CopyOnWriteHashMap<>();
        assertThat(a.computeIfPresent("a", (k, v) -> {
            throw new AssertionError();
        })).isNull();
        assertThat(a.put("a", "1")).isNull();
        assertThat(a.computeIfPresent("a", (k, v) -> {
            assertThat(k).isEqualTo("a");
            assertThat(v).isEqualTo("1");
            return "2";
        })).isEqualTo("2");
        assertThat(a.get("a")).isEqualTo("2");
    }

    @Test
    void merge() {
        Map<String, String> a = new CopyOnWriteHashMap<>();
        assertThat(a.merge("a", "1", (v1, v2) -> {
            throw new AssertionError();
        })).isEqualTo("1");
        assertThat(a.merge("a", "2", (v1, v2) -> {
            assertThat(v1).isEqualTo("1");
            assertThat(v2).isEqualTo("2");
            return "3";
        })).isEqualTo("3");
        assertThat(a.get("a")).isEqualTo("3");
    }

    @Test
    void hashEquals() {
        Map<String, String> a = new CopyOnWriteHashMap<>();
        a.put("a", "1");
        Map<String, String> b = new CopyOnWriteHashMap<>(a);
        assertThat(b).isEqualTo(a);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());

        b.put("a", "3");
        assertThat(b).isNotEqualTo(a);
        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
    }

    @Test
    void entrySetIterator() {
        Map<String, String> a = new CopyOnWriteHashMap<>();
        a.put("a", "1");
        Iterator<Entry<String, String>> iter = a.entrySet().iterator();
        assertThat(iter.hasNext()).isTrue();
        Entry<String, String> entry = iter.next();
        assertThat(entry).isEqualTo(new AbstractMap.SimpleImmutableEntry<>("a", "1"));
        assertThatThrownBy(() -> entry.setValue("2")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> iter.remove()).isInstanceOf(UnsupportedOperationException.class);
    }
}
