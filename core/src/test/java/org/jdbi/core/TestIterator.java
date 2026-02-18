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
package org.jdbi.core;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.h2.jdbc.JdbcSQLNonTransientException;
import org.jdbi.core.extension.ExtensionFactory;
import org.jdbi.core.extension.HandleSupplier;
import org.jdbi.core.internal.testing.H2DatabaseExtension;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.core.result.ResultIterator;
import org.jdbi.core.result.ResultSetException;
import org.jdbi.core.spi.JdbiPlugin;
import org.jdbi.core.statement.Query;
import org.jdbi.core.statement.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.jdbi.core.extension.ExtensionFactory.FactoryFlag.NON_VIRTUAL_FACTORY;

class TestIterator {

    static final Something ONE = new Something(3, "foo");
    static final Something TWO = new Something(4, "bar");
    static final Something THREE = new Something(5, "baz");

    @RegisterExtension
    H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(H2DatabaseExtension.SOMETHING_INITIALIZER)
            .withPlugin(new JdbiPlugin() {
                @Override
                public void customizeJdbi(Jdbi jdbi) {
                    jdbi.registerRowMapper(new SomethingMapper());
                    jdbi.registerExtension(new SpiffyFactory());
                }
            });

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

    static class SpiffyFactory implements ExtensionFactory {

        @Override
        public boolean accepts(Class<?> extensionType) {
            return extensionType == Spiffy.class;
        }

        @Override
        public <E> E attach(Class<E> extensionType, HandleSupplier handleSupplier) {
            return (E) new SpiffyImpl(handleSupplier);
        }

        @Override
        public Set<FactoryFlag> getFactoryFlags() {
            return EnumSet.of(NON_VIRTUAL_FACTORY);
        }
    }

    public interface Spiffy {

        default Iterator<Something> iterator() {
            return resultIterator();
        }

        ResultIterator<Something> resultIterator();

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

    static class SpiffyImpl implements Spiffy {

        private final HandleSupplier handleSupplier;

        SpiffyImpl(HandleSupplier handleSupplier) {
            this.handleSupplier = handleSupplier;
        }

        @Override
        public ResultIterator<Something> resultIterator() {
            Handle handle = handleSupplier.getHandle();
            Query query = handle.createQuery("SELECT id, name FROM something ORDER BY id DESC");

            // attach to the handle for cleanup, otherwise the statement will leak if the iterator does not
            // finish.
            return query.attachToHandleForCleanup().mapTo(Something.class).iterator();
        }

        @Override
        public void iterator(Consumer<Iterator<Something>> consumer) {
            Handle handle = handleSupplier.getHandle();
            try (Query query = handle.createQuery("SELECT id, name FROM something ORDER BY id DESC")) {
                query.mapTo(Something.class).useIterator(consumer::accept);
            }
        }
    }
}
