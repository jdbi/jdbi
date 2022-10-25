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
package org.jdbi.v3.core.result;

import java.util.Map;
import java.util.NoSuchElementException;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.statement.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestIterator {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.withSomething();

    private Handle h;

    @BeforeEach
    public void setUp() {
        h = h2Extension.openHandle();
    }

    @AfterEach
    public void doTearDown() {
        assertThat(h.isClosed()).withFailMessage("Handle was not closed correctly!").isTrue();
    }

    @Test
    public void testSimple() {
        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();
        h.createUpdate("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandleRollback()
            .mapToMap()
            .iterator();

        assertThat(it).hasNext();
        it.next();
        assertThat(it).hasNext();
        it.next();
        assertThat(it).hasNext();
        it.next();
        assertThat(it).isExhausted();
    }

    @Test
    public void testEmptyWorksToo() {
        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandleRollback()
            .mapToMap()
            .iterator();

        assertThat(it).isExhausted();
    }

    @Test
    public void testHasNext() {
        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();
        h.createUpdate("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandleRollback()
            .mapToMap()
            .iterator();

        assertThat(it).hasNext()
                      .hasNext()
                      .hasNext();
        it.next();
        assertThat(it).hasNext()
                      .hasNext()
                      .hasNext();
        it.next();
        assertThat(it).hasNext()
                      .hasNext()
                      .hasNext();
        it.next();
        assertThat(it).isExhausted()
                      .isExhausted()
                      .isExhausted();
    }

    @Test
    public void testNext() {
        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();
        h.createUpdate("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandleRollback()
            .mapToMap()
            .iterator();

        assertThat(it).hasNext();
        it.next();
        it.next();
        it.next();
        assertThat(it).isExhausted();
    }

    @Test
    public void testJustNext() {
        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();
        h.createUpdate("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandleRollback()
            .mapToMap()
            .iterator();

        assertThat(it.next()).containsEntry("name", "eric");
        assertThat(it.next()).containsEntry("name", "brian");
        assertThat(it.next()).containsEntry("name", "john");
    }

    @Test
    public void testTwoTwo() {
        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();
        h.createUpdate("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandleRollback()
            .mapToMap()
            .iterator();

        it.next();
        it.next();
        assertThat(it).hasNext()
                      .hasNext();
        it.next();
        assertThat(it).isExhausted()
                      .isExhausted();
    }

    @Test
    public void testTwoOne() {
        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();
        h.createUpdate("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandleRollback()
            .mapToMap()
            .iterator();

        assertThat(it).hasNext();
        it.next();
        it.next();
        assertThat(it).hasNext();
        it.next();
        assertThat(it).isExhausted();
    }

    @Test
    public void testExplodeIterator() {
        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();
        h.createUpdate("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandleRollback()
            .mapToMap()
            .iterator();

        assertThat(it).hasNext();
        it.next();
        assertThat(it).hasNext();
        it.next();
        assertThat(it).hasNext();
        it.next();
        assertThat(it).isExhausted();

        assertThatThrownBy(it::next).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testEmptyExplosion() {

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandleRollback()
            .mapToMap()
            .iterator();

        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    public void testNonPathologicalJustNext() {
        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();

        // this only works (and does not leak resources) because the iterator contains only a single
        // element and is closed (and resources released once that element is processed).
        final Map<String, Object> result = h.createQuery("select * from something order by id")
            .cleanupHandleRollback()
            .mapToMap()
            .iterator()
            .next();

        assertThat(result.get("id")).isEqualTo(1L);
        assertThat(result.get("name")).isEqualTo("eric");
    }

    @Test
    public void testManageResourcesOnQuery() {
        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();

        // this requires t-w-r to ensure that the resources of the query are released properly.
        // this could be done through the iterator as well (see next test).
        try (Query query = h.createQuery("select * from something order by id")) {
            final Map<String, Object> result = query.cleanupHandleRollback()
                .mapToMap()
                .iterator()
                .next();

            assertThat(result.get("id")).isEqualTo(1L);
            assertThat(result.get("name")).isEqualTo("eric");

            assertThat(h.isClosed()).isFalse();
        }
    }

    @Test
    public void testManageResourcesOnIterator() {
        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();

        ResultIterable<Map<String, Object>> iterable = h.createQuery("select * from something order by id")
            .cleanupHandleRollback()
            .mapToMap();

        // use t-w-r to ensure that the resources of the query are released properly.
        try (ResultIterator<Map<String, Object>> it = iterable.iterator()) {
            final Map<String, Object> result = it.next();

            assertThat(result.get("id")).isEqualTo(1L);
            assertThat(result.get("name")).isEqualTo("eric");

            assertThat(h.isClosed()).isFalse();
        }
    }
}
