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
    public void buildAppliesBothPluginHooksInOrder() {
        final AtomicInteger tick = new AtomicInteger();
        final int[] configuredAt = {-1};
        final int[] customizedAt = {-1};

        final JdbiPlugin plugin = new JdbiPlugin() {
            @Override
            public void configure(final Jdbi.Builder builder) {
                configuredAt[0] = tick.getAndIncrement();
            }

            @Override
            public void customizeJdbi(final Jdbi jdbi) {
                customizedAt[0] = tick.getAndIncrement();
            }
        };

        final Jdbi jdbi = Jdbi.builder(url()).installPlugin(plugin).build();

        assertThat(jdbi).isNotNull();
        // both hooks run, configure() before customizeJdbi()
        assertThat(configuredAt[0]).isZero();
        assertThat(customizedAt[0]).isOne();
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
    public void buildDrainsPluginsInstalledDuringConfigure() {
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
}
