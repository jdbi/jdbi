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
package org.jdbi.v3.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.result.ResultIterator;
import org.jdbi.v3.core.transaction.TransactionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestClosingHandle {
    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.withSomething();

    @Test
    public void testNotClosing() {
        Handle h = h2Extension.openHandle();

        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();

        List<Map<String, Object>> results = h.createQuery("select * from something order by id").mapToMap().list();
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).containsEntry("name", "eric");
        assertThat(h.isClosed()).isFalse();
    }

    @Test
    public void testClosing() {
        Handle h = h2Extension.openHandle();

        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();

        List<Map<String, Object>> results = h.createQuery("select * from something order by id")
            .cleanupHandleRollback()
            .mapToMap()
            .list();

        assertThat(results).hasSize(2);
        assertThat(results.get(0)).containsEntry("name", "eric");
        assertThat(h.isClosed()).isTrue();
    }

    @Test
    public void testIterateKeepHandle() {
        Handle h = h2Extension.openHandle();

        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .mapToMap()
            .iterator();

        assertThat(it).toIterable().hasSize(2);
        assertThat(h.isClosed()).isFalse();
    }

    @Test
    public void testIterateAllTheWay() {
        Handle h = h2Extension.openHandle();

        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandleRollback()
            .mapToMap()
            .iterator();

        assertThat(it).toIterable().hasSize(2);
        assertThat(h.isClosed()).isTrue();
    }

    @Test
    public void testIteratorBehaviour() {
        Handle h = h2Extension.openHandle();

        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();
        h.createUpdate("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandleRollback()
            .mapToMap()
            .iterator();

        assertThat(it.hasNext()).isTrue();
        assertThat(h.isClosed()).isFalse();
        it.next();
        assertThat(it.hasNext()).isTrue();
        assertThat(h.isClosed()).isFalse();
        it.next();
        assertThat(it.hasNext()).isTrue();
        assertThat(h.isClosed()).isFalse();
        it.next();
        assertThat(it.hasNext()).isFalse();
        assertThat(h.isClosed()).isTrue();
    }

    @Test
    public void testIteratorClose() {
        Handle h = h2Extension.openHandle();

        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();
        h.createUpdate("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandleRollback()
            .mapToMap()
            .iterator();

        assertThat(it.hasNext()).isTrue();
        assertThat(h.isClosed()).isFalse();
        it.next();
        assertThat(it.hasNext()).isTrue();
        assertThat(h.isClosed()).isFalse();

        it.close();
        assertThat(it.hasNext()).isFalse();
        assertThat(h.isClosed()).isTrue();
    }

    @Test
    public void testCloseWithOpenTransaction() {
        Handle h = h2Extension.openHandle();

        h.begin();

        assertThatThrownBy(h::close).isInstanceOf(TransactionException.class);
        assertThat(h.isClosed()).isTrue();
    }

    @Test
    public void testCloseWithOpenTransactionCheckDisabled() {
        Handle h = h2Extension.openHandle();

        h.getConfig(Handles.class).setForceEndTransactions(false);

        h.begin();

        h.close();
        assertThat(h.isClosed()).isTrue();
    }

    @Test
    public void testCloseWithOpenContainerManagedTransaction() throws Exception {
        Handle handle = null;
        try (Connection conn = DriverManager.getConnection(h2Extension.getUri())) {
            conn.setAutoCommit(false); // open transaction

            handle = Jdbi.open(conn);
            handle.close();
        }

        assertNotNull(handle);
        assertThat(handle.isClosed()).isTrue();
    }
}

