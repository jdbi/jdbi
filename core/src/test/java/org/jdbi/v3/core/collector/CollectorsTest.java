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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.statement.Query;
import org.junit.jupiter.api.BeforeEach;
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
        assertThat(queryString().set())
                .isInstanceOf(Set.class)
                .containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    void collectIntoLinkedList() {
        assertThat(baseQuery().collectInto(new GenericType<LinkedList<String>>() {}))
                .isInstanceOf(LinkedList.class)
                .containsExactly("a", "b", "c");
    }

    @Test
    void modifyListImpl() {
        assertThat(queryString().list())
                .isNotInstanceOf(LinkedList.class);

        assertThat(baseQuery()
                    .registerCollector(List.class, Collectors.toCollection(LinkedList::new))
                    .mapTo(String.class)
                    .list())
                .isInstanceOf(LinkedList.class)
                .containsExactly("a", "b", "c");

        assertThat(baseQuery()
                    .registerCollector(List.class, Collectors.toCollection(LinkedList::new))
                    .collectInto(new GenericType<ArrayList<String>>() {}))
                .isInstanceOf(ArrayList.class)
                .containsExactly("a", "b", "c");
    }

    private ResultIterable<String> queryString() {
        return baseQuery().mapTo(String.class);
    }

    private Query baseQuery() {
        return handle.createQuery("select * from collection");
    }
}
