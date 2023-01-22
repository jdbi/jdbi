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

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.h2.jdbc.JdbcSQLNonTransientException;
import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.result.ResultSetException;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TestStream {

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
    void testAttachStreamExhaust() {
        Spiffy dao = handle.attach(Spiffy.class);

        try (Stream<Something> stream = dao.stream()) {
            assertThat(stream).containsExactly(THREE, TWO, ONE);
        }
    }

    @Test
    void testAttachStreamSneaky() {
        Spiffy dao = handle.attach(Spiffy.class);

        try (Stream<Something> stream = dao.sneakyStream()) {
            assertThat(stream).containsExactly(THREE, TWO, ONE);
        }
    }

    @Test
    void testAttachStreamLeak() {
        Spiffy dao = handle.attach(Spiffy.class);

        Stream<Something> stream = dao.stream();
        assertThat(stream.findFirst()).isPresent().contains(THREE);
    }

    @Test
    void testAttachStreamCallbackExhaust() {
        Spiffy dao = handle.attach(Spiffy.class);

        dao.stream(stream -> assertThat(stream).containsExactly(THREE, TWO, ONE));
    }

    @Test
    void testAttachStreamCallbackLeak() {
        Spiffy dao = handle.attach(Spiffy.class);

        dao.stream(stream -> assertThat(stream.findFirst()).isPresent().contains(THREE));
    }

    //
    // on demand tests. Only consumption within a default method or using a callback works.
    //

    @Test
    void testOnDemandStreamExhaustFails() {
        Spiffy dao = jdbi.onDemand(Spiffy.class);

        assertResourcesClosed(() -> {
            try (Stream<Something> stream = dao.stream()) {
                assertThat(stream).containsExactly(THREE, TWO, ONE);
            }
        });
    }

    @Test
    void testOnDemandStreamSneakyFails() {
        Spiffy dao = jdbi.onDemand(Spiffy.class);

        assertResourcesClosed(() -> {
            try (Stream<Something> stream = dao.sneakyStream()) {
                assertThat(stream).containsExactly(THREE, TWO, ONE);
            }
        });
    }

    @Test
    void testOnDemandStreamWrapped() {
        Spiffy dao = jdbi.onDemand(Spiffy.class);

        dao.streamTester();
    }

    @Test
    void testOnDemandStreamWrappedLeak() {
        Spiffy dao = jdbi.onDemand(Spiffy.class);

        dao.streamLeakTester();
    }

    @Test
    void testOnDemandStreamCallbackExhaust() {
        Spiffy dao = jdbi.onDemand(Spiffy.class);

        dao.stream(stream -> assertThat(stream).containsExactly(THREE, TWO, ONE));
    }

    @Test
    void testOnDemandStreamCallbackLeak() {
        Spiffy dao = jdbi.onDemand(Spiffy.class);

        dao.stream(stream -> assertThat(stream.findFirst()).isPresent().contains(THREE));
    }

    //
    // extension tests
    //

    @Test
    void testExtensionStreamExhaust() {
        jdbi.useExtension(Spiffy.class,
                dao -> {
                    try (Stream<Something> stream = dao.stream()) {
                        assertThat(stream).containsExactly(THREE, TWO, ONE);
                    }
                });
    }

    @Test
    void testExtensionStreamSneaky() {
        jdbi.useExtension(Spiffy.class,
                dao -> {
                    try (Stream<Something> stream = dao.sneakyStream()) {
                        assertThat(stream).containsExactly(THREE, TWO, ONE);
                    }
                });
    }

    @Test
    void testExtensionStreamLeak() {
        jdbi.useExtension(Spiffy.class,
                dao -> {
                    Stream<Something> stream = dao.stream();
                    assertThat(stream.findFirst()).isPresent().contains(THREE);
                });
    }

    @Test
    void testExtensionStreamCallbackExhaust() {
        jdbi.useExtension(Spiffy.class,
                dao -> dao.stream(stream -> assertThat(stream).containsExactly(THREE, TWO, ONE)));
    }

    @Test
    void testExtensionStreamCallbackLeak() {
        jdbi.useExtension(Spiffy.class,
                dao -> {
                    Stream<Something> stream = dao.stream();
                    assertThat(stream.findFirst()).isPresent().contains(THREE);
                });
    }

    @Test
    void testExtensionStreamReturnFails() {

        assertResourcesClosed(() -> {
            try (Stream<Something> stream = jdbi.withExtension(Spiffy.class, Spiffy::stream)) {
                assertThat(stream).containsExactly(THREE, TWO, ONE);
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
    }

    public interface Spiffy {

        Stream<Something> stream();

        void stream(Consumer<Stream<Something>> consumer);

        default void streamTester() {
            try (Stream<Something> stream = stream()) {
                assertThat(stream).containsExactly(THREE, TWO, ONE);
            }
        }

        default void streamLeakTester() {
            Stream<Something> stream = stream();
            assertThat(stream.findFirst()).isPresent().contains(THREE);
        }

        default Stream<Something> sneakyStream() {
            return stream();
        }
    }

    static class SpiffyImpl implements Spiffy {

        private final HandleSupplier handleSupplier;

        SpiffyImpl(HandleSupplier handleSupplier) {
            this.handleSupplier = handleSupplier;
        }

        @Override
        public Stream<Something> stream() {
            Handle handle = handleSupplier.getHandle();
            Query query = handle.createQuery("SELECT id, name FROM something ORDER BY id DESC");

            // attach to the handle for cleanup, otherwise the statement will leak if the stream does not
            // finish.
            return query.attachToHandleForCleanup().mapTo(Something.class).stream();
        }

        public void stream(Consumer<Stream<Something>> consumer) {
            Handle handle = handleSupplier.getHandle();
            try (Query query = handle.createQuery("SELECT id, name FROM something ORDER BY id DESC")) {
                query.mapTo(Something.class).useStream(consumer::accept);
            }
        }
    }
}
