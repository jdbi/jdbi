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
package org.jdbi.v3.core.collector;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.statement.Query;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class CollectorsTest {

    @RegisterExtension
    H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    Handle handle;

    @BeforeEach
    void setup() {
        handle = h2Extension.getSharedHandle();
        handle.execute("create table collection (k varchar)");
        handle.execute("insert into collection (k) values('a')");
        handle.execute("insert into collection (k) values('b')");
        handle.execute("insert into collection (k) values('c')");
    }

    @Test
    void collectIntoSet() {
        queryString(r -> assertThat(r.set())
                .isInstanceOf(Set.class)
                .containsExactlyInAnyOrder("a", "b", "c"));
    }


    @Test
    void collectIntoList() {
        queryString(r -> assertThat(r.list())
                .isInstanceOf(List.class)
                .containsExactly("a", "b", "c"));
    }

    @Test
    void collectIntoGenericType() {
        GenericType<LinkedList<String>> type = new GenericType<LinkedList<String>>() {};

        try (Query query = baseQuery()) {
            assertThat(query.collectInto(type))
                    .isInstanceOf(LinkedList.class)
                    .containsExactly("a", "b", "c");
        }
    }


    @Test
    void collectIntoRegisteredListTypeUsingCollector() {
        queryString(r -> assertThat(r.list())
                .isNotInstanceOf(LinkedList.class));

        // register list type
        handle.registerCollector(List.class, Collectors.toCollection(LinkedList::new));

        // look up default list type manually
        GenericType<List<String>> type = new GenericType<List<String>>() {};
        Collector<?, ?, ?> collector = handle.getConfig(JdbiCollectors.class).findFor(type.getType())
                .orElseGet(Assertions::fail);

        try (Query query = baseQuery()) {
            assertThat(query
                    .mapTo(String.class)
                    // old school, collect using a collector
                    .collect((Collector<String, ?, List<String>>) collector))
                    .isInstanceOf(LinkedList.class)
                    .containsExactly("a", "b", "c");
        }
    }

    void collectIntoRegisteredListTypeDirectly() {
        queryString(r -> assertThat(r.list()).isNotInstanceOf(LinkedList.class));

        // register list type
        handle.registerCollector(List.class, Collectors.toCollection(LinkedList::new));

        try (Query query = baseQuery()) {
            assertThat((List<String>) query
                    // use the registered list type
                    .collectInto(GenericTypes.parameterizeClass(List.class, String.class)))
                    .isInstanceOf(LinkedList.class)
                    .containsExactly("a", "b", "c");
        }
    }


    @Test
    @Disabled("Registry lookup is not implemented yet")
    void listIntoRegisteredListType() {
        queryString(r -> assertThat(r.list()).isNotInstanceOf(LinkedList.class));

        // register list type
        handle.getConfig(JdbiCollectors.class)
                .registerCollector(List.class, Collectors.toCollection(LinkedList::new));

        try (Query query = baseQuery()) {
            assertThat(query
                    .mapTo(String.class)
                    // use the registered list type
                    .list())
                    .isInstanceOf(LinkedList.class)
                    .containsExactly("a", "b", "c");
        }
    }

    @Test
    @Disabled("Registry lookup is not implemented yet")
    void listIntoDefaultRegisteredListType() {
        queryString(r -> assertThat(r.list()).isNotInstanceOf(LinkedList.class));

        try (Query query = baseQuery()) {
            assertThat(query
                    .mapTo(String.class)
                    // no registered list type, fall back to default type.
                    .list())
                    .isNotInstanceOf(LinkedList.class)
                    .containsExactly("a", "b", "c");
        }
    }

    @Test
    void collectIntoRegisteredSetTypeDirectly() {
        queryString(r -> assertThat(r.set()).isNotInstanceOf(LinkedHashSet.class));

        // register list type
        handle.registerCollector(Set.class, Collectors.toCollection(LinkedHashSet::new));

        try (Query query = baseQuery()) {
            assertThat((Set<String>) query
                    // use the registered set type
                    .collectInto(GenericTypes.parameterizeClass(Set.class, String.class)))
                    .isInstanceOf(LinkedHashSet.class)
                    .containsExactlyInAnyOrder("a", "b", "c");
        }
    }

    @Test
    @Disabled("Registry lookup is not implemented yet")
    void setIntoRegisteredSetType() {
        queryString(r -> assertThat(r.set()).isNotInstanceOf(LinkedHashSet.class));

        // register set type
        handle.registerCollector(Set.class, Collectors.toCollection(LinkedHashSet::new));

        try (Query query = baseQuery()) {
            assertThat(query
                    .mapTo(String.class)
                    // use the registered set type
                    .set())
                    .isInstanceOf(LinkedHashSet.class)
                    .containsExactlyInAnyOrder("a", "b", "c");
        }
    }

    @Test
    @Disabled("Registry lookup is not implemented yet")
    void setIntoDefaultRegisteredSetType() {
        queryString(r -> assertThat(r.set()).isNotInstanceOf(LinkedHashSet.class));

        try (Query query = baseQuery()) {
            assertThat(query
                    .mapTo(String.class)
                    // no registered set type, fall back to default type.
                    .set())
                    .isNotInstanceOf(LinkedHashSet.class)
                    .containsExactlyInAnyOrder("a", "b", "c");
        }
    }

    private void queryString(Consumer<ResultIterable<String>> callback) {
        try (Query query = baseQuery()) {
            callback.accept(query.mapTo(String.class));
        }
    }

    private Query baseQuery() {
        return handle.createQuery("select * from collection");
    }
}
