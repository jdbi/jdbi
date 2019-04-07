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

import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.core.transaction.TransactionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jdbi.v3.core.transaction.TransactionIsolationLevel.READ_COMMITTED;
import static org.jdbi.v3.core.transaction.TransactionIsolationLevel.READ_UNCOMMITTED;

public class TestJdbiNestedCallBehavior {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule()
        .withPlugin(new TestPlugin())
        .withSomething();

    static class TestPlugin implements JdbiPlugin {
        @Override
        public void customizeJdbi(Jdbi jdbi) {
            jdbi.registerExtension(new TestExtensionFactory());
        }
    }

    static class TestExtensionFactory implements ExtensionFactory {
        @Override
        public boolean accepts(Class<?> extensionType) {
            return TestExtension.class.equals(extensionType);
        }

        @Override
        public <E> E attach(Class<E> extensionType, HandleSupplier handle) {
            return extensionType.cast(new TestExtensionImpl(handle));
        }
    }

    public interface TestExtension {
        Handle getHandle();

        default void run(Runnable runnable) {
            runnable.run();
        }
    }

    public static class TestExtensionImpl implements TestExtension {
        private final HandleSupplier handleSupplier;

        TestExtensionImpl(HandleSupplier handleSupplier) {
            this.handleSupplier = handleSupplier;
        }

        @Override
        public Handle getHandle() {
            return handleSupplier.getHandle();
        }
    }

    private Jdbi jdbi;
    private TestExtension onDemand;

    @Before
    public void setUp() {
        jdbi = dbRule.getJdbi();
        onDemand = jdbi.onDemand(TestExtension.class);
    }

    @Test
    public void nestedUseHandle() {
        jdbi.useHandle(h1 ->
            jdbi.useHandle(h2 ->
                assertThat(h1).isSameAs(h2)));

        jdbi.withHandle(h1 -> {
            jdbi.useHandle(h2 ->
                assertThat(h1).isSameAs(h2));
            return null;
        });

        jdbi.useTransaction(h1 ->
            jdbi.useHandle(h2 ->
                assertThat(h1).isSameAs(h2)));
        jdbi.useTransaction(READ_COMMITTED, h1 ->
            jdbi.useHandle(h2 ->
                assertThat(h1).isSameAs(h2)));

        jdbi.inTransaction(h1 -> {
            jdbi.useHandle(h2 ->
                assertThat(h1).isSameAs(h2));
            return null;
        });
        jdbi.inTransaction(READ_COMMITTED, h1 -> {
            jdbi.useHandle(h2 ->
                assertThat(h1).isSameAs(h2));
            return null;
        });

        jdbi.useExtension(TestExtension.class, e ->
            jdbi.useHandle(h ->
                assertThat(e.getHandle()).isSameAs(h)));

        jdbi.withExtension(TestExtension.class, e -> {
            jdbi.useHandle(h ->
                assertThat(e.getHandle()).isSameAs(h));
            return null;
        });

        onDemand.run(() ->
            jdbi.useHandle(h ->
                assertThat(onDemand.getHandle()).isSameAs(h)));
    }

    @Test
    public void nestedWithHandle() throws Exception {
        jdbi.useHandle(h1 ->
            jdbi.withHandle(h2 ->
                assertThat(h1).isSameAs(h2)));

        jdbi.withHandle(h1 ->
            jdbi.withHandle(h2 ->
                assertThat(h1).isSameAs(h2)));

        jdbi.useTransaction(h1 ->
            jdbi.withHandle(h2 ->
                assertThat(h1).isSameAs(h2)));
        jdbi.useTransaction(READ_COMMITTED, h1 ->
            jdbi.withHandle(h2 ->
                assertThat(h1).isSameAs(h2)));

        jdbi.inTransaction(h1 ->
            jdbi.withHandle(h2 ->
                assertThat(h1).isSameAs(h2)));
        jdbi.inTransaction(READ_COMMITTED, h1 ->
            jdbi.withHandle(h2 ->
                assertThat(h1).isSameAs(h2)));

        jdbi.useExtension(TestExtension.class, e ->
            jdbi.withHandle(h ->
                assertThat(e.getHandle()).isSameAs(h)));

        jdbi.withExtension(TestExtension.class, e ->
            jdbi.withHandle(h ->
                assertThat(e.getHandle()).isSameAs(h)));

        onDemand.run(() ->
            jdbi.withHandle(h ->
                assertThat(onDemand.getHandle()).isSameAs(h)));
    }

    @Test
    public void nestedUseTransaction() {
        jdbi.useHandle(h1 ->
            jdbi.useTransaction(h2 ->
                assertThat(h1).isSameAs(h2)));
        jdbi.useHandle(h1 ->
            jdbi.useTransaction(READ_COMMITTED, h2 ->
                assertThat(h1).isSameAs(h2)));

        jdbi.withHandle(h1 -> {
            jdbi.useTransaction(h2 ->
                assertThat(h1).isSameAs(h2));
            return null;
        });
        jdbi.withHandle(h1 -> {
            jdbi.useTransaction(READ_COMMITTED, h2 ->
                assertThat(h1).isSameAs(h2));
            return null;
        });

        jdbi.useTransaction(h1 ->
            jdbi.useTransaction(h2 ->
                assertThat(h1).isSameAs(h2)));
        jdbi.useTransaction(READ_COMMITTED, h1 ->
            jdbi.useTransaction(h2 ->
                assertThat(h1).isSameAs(h2)));
        jdbi.useTransaction(h1 ->
            jdbi.useTransaction(READ_COMMITTED, h2 ->
                assertThat(h1).isSameAs(h2)));
        jdbi.useTransaction(READ_COMMITTED, h1 ->
            jdbi.useTransaction(READ_COMMITTED, h2 ->
                assertThat(h1).isSameAs(h2)));
        assertThatThrownBy(() ->
            jdbi.useTransaction(READ_COMMITTED, h1 ->
                jdbi.useTransaction(READ_UNCOMMITTED, h2 ->
                    assertThat(h1).isSameAs(h2))))
            .isInstanceOf(TransactionException.class)
            .hasMessageContaining("already running in a transaction with isolation level READ_COMMITTED");

        jdbi.inTransaction(h1 -> {
            jdbi.useTransaction(h2 ->
                assertThat(h1).isSameAs(h2));
            return null;
        });
        jdbi.inTransaction(READ_COMMITTED, h1 -> {
            jdbi.useTransaction(h2 ->
                assertThat(h1).isSameAs(h2));
            return null;
        });
        jdbi.inTransaction(h1 -> {
            jdbi.useTransaction(READ_COMMITTED, h2 ->
                assertThat(h1).isSameAs(h2));
            return null;
        });
        jdbi.inTransaction(READ_COMMITTED, h1 -> {
            jdbi.useTransaction(READ_COMMITTED, h2 ->
                assertThat(h1).isSameAs(h2));
            return null;
        });
        assertThatThrownBy(() ->
            jdbi.inTransaction(READ_COMMITTED, h1 -> {
                jdbi.useTransaction(READ_UNCOMMITTED, h2 ->
                    assertThat(h1).isSameAs(h2));
                return null;
            }))
            .isInstanceOf(TransactionException.class)
            .hasMessageContaining("already running in a transaction with isolation level READ_COMMITTED");

        jdbi.useExtension(TestExtension.class, e ->
            jdbi.useTransaction(h ->
                assertThat(e.getHandle()).isSameAs(h)));
        jdbi.useExtension(TestExtension.class, e ->
            jdbi.useTransaction(READ_COMMITTED, h ->
                assertThat(e.getHandle()).isSameAs(h)));

        jdbi.withExtension(TestExtension.class, e -> {
            jdbi.useTransaction(h ->
                assertThat(e.getHandle()).isSameAs(h));
            return null;
        });
        jdbi.withExtension(TestExtension.class, e -> {
            jdbi.useTransaction(READ_COMMITTED, h ->
                assertThat(e.getHandle()).isSameAs(h));
            return null;
        });

        onDemand.run(() ->
            jdbi.useTransaction(h ->
                assertThat(onDemand.getHandle()).isSameAs(h)));
        onDemand.run(() ->
            jdbi.useTransaction(READ_COMMITTED, h ->
                assertThat(onDemand.getHandle()).isSameAs(h)));
    }

    @Test
    public void nestedInTransaction() throws Exception {
        jdbi.useHandle(h1 ->
            jdbi.inTransaction(h2 ->
                assertThat(h1).isSameAs(h2)));

        jdbi.withHandle(h1 ->
            jdbi.inTransaction(h2 ->
                assertThat(h1).isSameAs(h2)));

        jdbi.useTransaction(h1 ->
            jdbi.inTransaction(h2 ->
                assertThat(h1).isSameAs(h2)));
        jdbi.useTransaction(READ_COMMITTED, h1 ->
            jdbi.inTransaction(h2 ->
                assertThat(h1).isSameAs(h2)));
        jdbi.useTransaction(h1 ->
            jdbi.inTransaction(READ_COMMITTED, h2 ->
                assertThat(h1).isSameAs(h2)));
        jdbi.useTransaction(READ_COMMITTED, h1 ->
            jdbi.inTransaction(READ_COMMITTED, h2 ->
                assertThat(h1).isSameAs(h2)));

        jdbi.inTransaction(h1 ->
            jdbi.inTransaction(h2 ->
                assertThat(h1).isSameAs(h2)));
        jdbi.inTransaction(READ_COMMITTED, h1 ->
            jdbi.inTransaction(h2 ->
                assertThat(h1).isSameAs(h2)));
        jdbi.inTransaction(h1 ->
            jdbi.inTransaction(READ_COMMITTED, h2 ->
                assertThat(h1).isSameAs(h2)));
        jdbi.inTransaction(READ_COMMITTED, h1 ->
            jdbi.inTransaction(READ_COMMITTED, h2 ->
                assertThat(h1).isSameAs(h2)));

        jdbi.useExtension(TestExtension.class, e ->
            jdbi.inTransaction(h ->
                assertThat(e.getHandle()).isSameAs(h)));

        jdbi.withExtension(TestExtension.class, e ->
            jdbi.inTransaction(h ->
                assertThat(e.getHandle()).isSameAs(h)));

        onDemand.run(() ->
            jdbi.inTransaction(h ->
                assertThat(onDemand.getHandle()).isSameAs(h)));
    }

    @Test
    public void nestedUseExtension() {
        jdbi.useHandle(h ->
            jdbi.useExtension(TestExtension.class, e ->
                assertThat(h).isSameAs(e.getHandle())));

        jdbi.withHandle(h -> {
            jdbi.useExtension(TestExtension.class, e ->
                assertThat(h).isSameAs(e.getHandle()));
            return null;
        });

        jdbi.useTransaction(h ->
            jdbi.useExtension(TestExtension.class, e ->
                assertThat(h).isSameAs(e.getHandle())));
        jdbi.useTransaction(READ_COMMITTED, h ->
            jdbi.useExtension(TestExtension.class, e ->
                assertThat(h).isSameAs(e.getHandle())));

        jdbi.inTransaction(h -> {
            jdbi.useExtension(TestExtension.class, e ->
                assertThat(h).isSameAs(e.getHandle()));
            return null;
        });
        jdbi.inTransaction(READ_COMMITTED, h -> {
            jdbi.useExtension(TestExtension.class, e ->
                assertThat(h).isSameAs(e.getHandle()));
            return null;
        });

        jdbi.useExtension(TestExtension.class, e1 ->
            jdbi.useExtension(TestExtension.class, e2 ->
                assertThat(e1.getHandle()).isSameAs(e2.getHandle())));

        jdbi.withExtension(TestExtension.class, e1 -> {
            jdbi.useExtension(TestExtension.class, e2 ->
                assertThat(e1.getHandle()).isSameAs(e2.getHandle()));
            return null;
        });

        onDemand.run(() ->
            jdbi.useExtension(TestExtension.class, e ->
                assertThat(onDemand.getHandle()).isSameAs(e.getHandle())));
    }

    @Test
    public void nestedWithExtension() throws Exception {
        jdbi.useHandle(h ->
            jdbi.withExtension(TestExtension.class, e ->
                assertThat(h).isSameAs(e.getHandle())));

        jdbi.withHandle(h ->
            jdbi.withExtension(TestExtension.class, e ->
                assertThat(h).isSameAs(e.getHandle())));

        jdbi.useTransaction(h ->
            jdbi.withExtension(TestExtension.class, e ->
                assertThat(h).isSameAs(e.getHandle())));
        jdbi.useTransaction(READ_COMMITTED, h ->
            jdbi.withExtension(TestExtension.class, e ->
                assertThat(h).isSameAs(e.getHandle())));

        jdbi.inTransaction(h ->
            jdbi.withExtension(TestExtension.class, e ->
                assertThat(h).isSameAs(e.getHandle())));
        jdbi.inTransaction(READ_COMMITTED, h ->
            jdbi.withExtension(TestExtension.class, e ->
                assertThat(h).isSameAs(e.getHandle())));

        jdbi.useExtension(TestExtension.class, e1 ->
            jdbi.withExtension(TestExtension.class, e2 ->
                assertThat(e1.getHandle()).isSameAs(e2.getHandle())));

        jdbi.withExtension(TestExtension.class, e1 ->
            jdbi.withExtension(TestExtension.class, e2 ->
                assertThat(e1.getHandle()).isSameAs(e2.getHandle())));

        onDemand.run(() ->
            jdbi.withExtension(TestExtension.class, e ->
                assertThat(onDemand.getHandle()).isSameAs(e.getHandle())));
    }

    @Test
    public void nestedOnDemand() {
        jdbi.useHandle(h ->
            onDemand.run(() ->
                assertThat(h).isSameAs(onDemand.getHandle())));

        jdbi.withHandle(h -> {
            onDemand.run(() ->
                assertThat(h).isSameAs(onDemand.getHandle()));
            return null;
        });

        jdbi.useTransaction(h ->
            onDemand.run(() ->
                assertThat(h).isSameAs(onDemand.getHandle())));
        jdbi.useTransaction(READ_COMMITTED, h ->
            onDemand.run(() ->
                assertThat(h).isSameAs(onDemand.getHandle())));

        jdbi.inTransaction(h -> {
            onDemand.run(() ->
                assertThat(h).isSameAs(onDemand.getHandle()));
            return null;
        });
        jdbi.inTransaction(READ_COMMITTED, h -> {
            onDemand.run(() ->
                assertThat(h).isSameAs(onDemand.getHandle()));
            return null;
        });

        jdbi.useExtension(TestExtension.class, e ->
            onDemand.run(() ->
                assertThat(e.getHandle()).isSameAs(onDemand.getHandle())));

        jdbi.withExtension(TestExtension.class, e -> {
            onDemand.run(() ->
                assertThat(e.getHandle()).isSameAs(onDemand.getHandle()));
            return null;
        });

        TestExtension onDemand2 = jdbi.onDemand(TestExtension.class);
        onDemand.run(() ->
            onDemand2.run(() ->
                assertThat(onDemand.getHandle()).isSameAs(onDemand2.getHandle())));
    }
}
