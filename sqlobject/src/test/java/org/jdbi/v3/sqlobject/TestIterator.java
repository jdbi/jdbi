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
package org.jdbi.v3.sqlobject;

import java.util.Iterator;
import java.util.function.Consumer;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.h2.jdbc.JdbcSQLNonTransientException;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.result.ResultIterator;
import org.jdbi.v3.core.result.ResultSetException;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TestIterator {

    static final Something ONE = new Something(3, "foo");
    static final Something TWO = new Something(4, "bar");
    static final Something THREE = new Something(5, "baz");

    @RegisterExtension
    JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    Handle handle;
    Jdbi jdbi;

    @BeforeEach
    void setUp() {
        jdbi = h2Extension.getJdbi();
        handle = h2Extension.getSharedHandle();

        insert(ONE);
        insert(TWO);
        insert(THREE);
    }

    private void insert(Something something) {
        try (Update update = handle.createUpdate("INSERT INTO something (id, name) VALUES (:id, :name)")) {
            update.bindBean(something)
                    .execute();
        }
    }

    private void assertResourcesClosed(ThrowingCallable callable) {
        assertThatExceptionOfType(ResultSetException.class).isThrownBy(callable)
                .withMessageContaining("Unable to advance result set")
                .havingCause()
                .isInstanceOf(JdbcSQLNonTransientException.class)
                .hasFieldOrPropertyWithValue("errorCode", 90007);
    }

    //
    // attach tests. Attach used an explicit handle, so these are simple
    //

    @Test
    void testAttachResultIteratorExhaust() {
        Spiffy dao = handle.attach(Spiffy.class);

        try (ResultIterator<Something> it = dao.resultIterator()) {
            assertThat(it).toIterable().containsExactly(THREE, TWO, ONE);
        }
    }

    @Test
    void testAttachResultIteratorSneaky() {
        Spiffy dao = handle.attach(Spiffy.class);

        try (ResultIterator<Something> it = dao.sneakyIterator()) {
            assertThat(it).toIterable().containsExactly(THREE, TWO, ONE);
        }
    }

    @Test
    void testAttachIteratorExhaust() {
        Spiffy dao = handle.attach(Spiffy.class);

        Iterator<Something> it = dao.iterator();
        assertThat(it).toIterable().containsExactly(THREE, TWO, ONE);
    }

    @Test
    void testAttachIteratorLeak() {
        Spiffy dao = handle.attach(Spiffy.class);

        // not closed, but does not leak because the handle close catches the resources
        Iterator<Something> it = dao.iterator();
        assertThat(it).hasNext();
        assertThat(it.next()).isEqualTo(THREE);
    }

    @Test
    void testAttachIteratorCallbackExhaust() {
        Spiffy dao = handle.attach(Spiffy.class);

        dao.iterator(it -> assertThat(it).toIterable().containsExactly(THREE, TWO, ONE));
    }

    @Test
    void testAttachIteratorCallbackLeak() {
        Spiffy dao = handle.attach(Spiffy.class);

        dao.iterator(it -> {
            assertThat(it).hasNext();
            assertThat(it.next()).isEqualTo(THREE);
        });
    }

    //
    // on demand tests. Only consumption within a default method or using a callback works.
    //

    @Test
    void testOnDemandResultIteratorExhaustFails() {
        Spiffy dao = jdbi.onDemand(Spiffy.class);

        assertResourcesClosed(() -> {
            try (ResultIterator<Something> it = dao.resultIterator()) {
                assertThat(it).toIterable().containsExactly(THREE, TWO, ONE);
            }
        });
    }

    @Test
    void testOnDemandIteratorExhaustFails() {
        Spiffy dao = jdbi.onDemand(Spiffy.class);

        assertResourcesClosed(() -> {
            Iterator<Something> it = dao.resultIterator();
            assertThat(it).toIterable().containsExactly(THREE, TWO, ONE);
        });
    }

    @Test
    void testOnDemandResultIteratorSneakyFails() {
        Spiffy dao = jdbi.onDemand(Spiffy.class);

        assertResourcesClosed(() -> {
            try (ResultIterator<Something> it = dao.sneakyIterator()) {
                assertThat(it).toIterable().containsExactly(THREE, TWO, ONE);
            }
        });
    }

    @Test
    void testOnDemandIteratorWrapped() {
        Spiffy dao = jdbi.onDemand(Spiffy.class);

        dao.iteratorTester();
    }

    @Test
    void testOnDemandResultIteratorWrapped() {
        Spiffy dao = jdbi.onDemand(Spiffy.class);

        dao.resultIteratorTester();
    }

    @Test
    void testOnDemandIteratorWrappedLeak() {
        Spiffy dao = jdbi.onDemand(Spiffy.class);

        dao.iteratorLeakTester();
    }

    @Test
    void testOnDemandIteratorCallbackExhaust() {
        Spiffy dao = jdbi.onDemand(Spiffy.class);

        dao.iterator(it -> assertThat(it).toIterable().containsExactly(THREE, TWO, ONE));
    }

    @Test
    void testOnDemandIteratorCallbackLeak() {
        Spiffy dao = jdbi.onDemand(Spiffy.class);

        dao.iterator(it -> {
            assertThat(it).hasNext();
            assertThat(it.next()).isEqualTo(THREE);
        });
    }

    //
    // extension tests
    //

    @Test
    void testExtensionResultIteratorExhaust() {
        jdbi.useExtension(Spiffy.class,
                dao -> {
                    try (ResultIterator<Something> it = dao.resultIterator()) {
                        assertThat(it).toIterable().containsExactly(THREE, TWO, ONE);
                    }
                });
    }

    @Test
    void testExtensionResultIteratorSneaky() {
        jdbi.useExtension(Spiffy.class,
                dao -> {
                    try (ResultIterator<Something> it = dao.sneakyIterator()) {
                        assertThat(it).toIterable().containsExactly(THREE, TWO, ONE);
                    }
                });
    }

    @Test
    void testExtensionIteratorExhaust() {
        jdbi.useExtension(Spiffy.class,
                dao -> {
                    Iterator<Something> it = dao.iterator();
                    assertThat(it).toIterable().containsExactly(THREE, TWO, ONE);
                });
    }

    @Test
    void testExtensionIteratorLeak() {
        jdbi.useExtension(Spiffy.class,
                dao -> {
                    Iterator<Something> it = dao.iterator();
                    assertThat(it).hasNext();
                    assertThat(it.next()).isEqualTo(THREE);
                });
    }

    @Test
    void testExtensionIteratorCallbackExhaust() {
        jdbi.useExtension(Spiffy.class,
                dao -> dao.iterator(it -> assertThat(it).toIterable().containsExactly(THREE, TWO, ONE)));
    }

    @Test
    void testExtensionIteratorCallbackLeak() {
        jdbi.useExtension(Spiffy.class,
                dao -> dao.iterator(it -> {
                    assertThat(it).hasNext();
                    assertThat(it.next()).isEqualTo(THREE);
                }));
    }

    @Test
    void testExtensionIteratorReturnFails() {

        assertResourcesClosed(() -> {
            // try to sneak out iterator through the extension callback
            try (ResultIterator<Something> it = jdbi.withExtension(Spiffy.class, Spiffy::resultIterator)) {
                assertThat(it).toIterable().containsExactly(THREE, TWO, ONE);
            }
        });
    }

    public interface Spiffy {

        @SqlQuery("select id, name from something order by id desc")
        @UseRowMapper(SomethingMapper.class)
        Iterator<Something> iterator();

        @SqlQuery("select id, name from something order by id desc")
        @UseRowMapper(SomethingMapper.class)
        ResultIterator<Something> resultIterator();

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@BindBean Something something);

        @SqlQuery("select id, name from something order by id desc")
        @UseRowMapper(SomethingMapper.class)
        void iterator(Consumer<Iterator<Something>> consumer);

        default void resultIteratorTester() {
            try (ResultIterator<Something> it = resultIterator()) {
                assertThat(it).toIterable().containsExactly(THREE, TWO, ONE);
            }
        }

        default void iteratorTester() {
            Iterator<Something> it = iterator();
            assertThat(it).toIterable().containsExactly(THREE, TWO, ONE);
        }

        default void iteratorLeakTester() {
            Iterator<Something> it = iterator();
            assertThat(it).hasNext();
            assertThat(it.next()).isEqualTo(THREE);
        }

        default ResultIterator<Something> sneakyIterator() {
            return resultIterator();
        }
    }
}
