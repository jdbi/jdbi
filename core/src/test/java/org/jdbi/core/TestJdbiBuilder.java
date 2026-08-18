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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.spi.JdbiPlugin;
import org.jdbi.core.transaction.LocalTransactionHandler;
import org.jdbi.core.transaction.TransactionHandler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestJdbiBuilder {

    private static String url() {
        return "jdbc:h2:mem:" + UUID.randomUUID();
    }

    @Test
    public void buildAppliesConfiguration() {
        final RowMapper<String> mapper = (rs, ctx) -> "mapped";
        final Jdbi jdbi = Jdbi.builder(url())
                .registerRowMapper(String.class, mapper)
                .build();

        try (Handle h = jdbi.open()) {
            assertThat(h.createQuery("select 1").mapTo(String.class).one()).isEqualTo("mapped");
        }
    }

    @Test
    public void buildSetsKnobs() {
        final TransactionHandler handler = LocalTransactionHandler.binding();
        final Jdbi jdbi = Jdbi.builder(url())
                .transactionHandler(handler)
                .build();

        assertThat(jdbi.getTransactionHandler()).isSameAs(handler);
    }

    @Test
    public void pluginConfigureContributesConfig() {
        final RowMapper<String> mapper = (rs, ctx) -> "from-plugin";
        final JdbiPlugin plugin = new JdbiPlugin() {
            @Override
            public void configure(final Jdbi.Builder builder) {
                builder.registerRowMapper(String.class, mapper);
            }
        };

        final Jdbi jdbi = Jdbi.builder(url()).installPlugin(plugin).build();

        try (Handle h = jdbi.open()) {
            assertThat(h.createQuery("select 1").mapTo(String.class).one()).isEqualTo("from-plugin");
        }
    }

    @Test
    public void ofRunsConsumerAtBuildTime() {
        final RowMapper<String> mapper = (rs, ctx) -> "from-of";
        final Jdbi jdbi = Jdbi.builder(url())
                .installPlugin(JdbiPlugin.of(b -> b.registerRowMapper(String.class, mapper)))
                .build();

        try (Handle h = jdbi.open()) {
            assertThat(h.createQuery("select 1").mapTo(String.class).one()).isEqualTo("from-of");
        }
    }

    @Test
    public void appliesSubPluginInstalledDuringConfigure() {
        final RowMapper<String> mapper = (rs, ctx) -> "from-sub";
        final JdbiPlugin sub = JdbiPlugin.of(b -> b.registerRowMapper(String.class, mapper));
        final JdbiPlugin parent = new JdbiPlugin() {
            @Override
            public void configure(final Jdbi.Builder builder) {
                builder.installPlugin(sub);
            }
        };

        final Jdbi jdbi = Jdbi.builder(url()).installPlugin(parent).build();

        try (Handle h = jdbi.open()) {
            assertThat(h.createQuery("select 1").mapTo(String.class).one()).isEqualTo("from-sub");
        }
    }

    @Test
    public void installPluginAppliesPluginOnce() {
        final AtomicInteger configureCount = new AtomicInteger();
        final JdbiPlugin plugin = new JdbiPlugin.Singleton() {
            @Override
            public void configure(final Jdbi.Builder builder) {
                configureCount.incrementAndGet();
            }
        };

        Jdbi.builder(url()).installPlugin(plugin).installPlugin(plugin).build();

        assertThat(configureCount).hasValue(1);
    }

    @Test
    public void buildAppliesPluginPulledInByTwoOthersOnce() {
        final AtomicInteger configureCount = new AtomicInteger();
        final JdbiPlugin shared = new JdbiPlugin() {
            @Override
            public void configure(final Jdbi.Builder builder) {
                configureCount.incrementAndGet();
            }
        };
        final JdbiPlugin first = new JdbiPlugin() {
            @Override
            public void configure(final Jdbi.Builder builder) {
                builder.installPlugin(shared);
            }
        };
        final JdbiPlugin second = new JdbiPlugin() {
            @Override
            public void configure(final Jdbi.Builder builder) {
                builder.installPlugin(shared);
            }
        };

        Jdbi.builder(url()).installPlugin(first).installPlugin(second).build();

        assertThat(configureCount).hasValue(1);
    }

    @Test
    public void callerConfigRegisteredAfterPluginTakesPrecedence() {
        // A plugin registers a mapper for String; the caller registers a different one AFTER installing the
        // plugin. installPlugin applies the plugin immediately, so the caller's later registration wins -- the
        // install-order precedence of Jdbi 3, not the "plugins always last" inversion of the deferred model.
        final JdbiPlugin plugin = JdbiPlugin.of(b -> b.registerRowMapper(String.class, (rs, ctx) -> "from-plugin"));
        final Jdbi jdbi = Jdbi.builder(url())
                .installPlugin(plugin)
                .registerRowMapper(String.class, (rs, ctx) -> "from-caller")
                .build();

        try (Handle h = jdbi.open()) {
            assertThat(h.createQuery("select 1").mapTo(String.class).one()).isEqualTo("from-caller");
        }
    }

    @Test
    public void toBuilderCopiesConfigurationAndKnobs() {
        final RowMapper<String> mapper = (rs, ctx) -> "base";
        final TransactionHandler handler = LocalTransactionHandler.binding();
        final Jdbi base = Jdbi.builder(url())
                .registerRowMapper(String.class, mapper)
                .transactionHandler(handler)
                .build();

        final Jdbi derived = base.toBuilder().build();

        assertThat(derived.getTransactionHandler()).isSameAs(handler);
        try (Handle h = derived.open()) {
            assertThat(h.createQuery("select 1").mapTo(String.class).one()).isEqualTo("base");
        }
    }

    @Test
    public void toBuilderConfigurationIsIndependentOfSource() {
        final Jdbi base = Jdbi.builder(url())
                .registerRowMapper(String.class, (rs, ctx) -> "base")
                .build();

        final Jdbi derived = base.toBuilder()
                .registerRowMapper(String.class, (rs, ctx) -> "derived")
                .build();

        try (Handle b = base.open(); Handle d = derived.open()) {
            assertThat(b.createQuery("select 1").mapTo(String.class).one()).isEqualTo("base");
            assertThat(d.createQuery("select 1").mapTo(String.class).one()).isEqualTo("derived");
        }
    }

    @Test
    public void toBuilderDoesNotReapplySeededPlugins() {
        final AtomicInteger configureCount = new AtomicInteger();
        final RowMapper<String> mapper = (rs, ctx) -> "from-plugin";
        final JdbiPlugin plugin = new JdbiPlugin() {
            @Override
            public void configure(final Jdbi.Builder builder) {
                configureCount.incrementAndGet();
                builder.registerRowMapper(String.class, mapper);
            }
        };

        final Jdbi base = Jdbi.builder(url()).installPlugin(plugin).build();
        assertThat(configureCount).hasValue(1);

        final Jdbi derived = base.toBuilder().build();

        // The seeded plugin is already applied: its configuration is carried in the copied config, so build()
        // does not re-run configure(), but its effect is present on the derived instance.
        assertThat(configureCount).hasValue(1);
        try (Handle h = derived.open()) {
            assertThat(h.createQuery("select 1").mapTo(String.class).one()).isEqualTo("from-plugin");
        }
    }
}
