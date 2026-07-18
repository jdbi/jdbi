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
import java.util.function.Consumer;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.mapper.ColumnMappers;
import org.jdbi.core.mapper.NoSuchMapperException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestOpenWithConfigScope {

    private static String url() {
        return "jdbc:h2:mem:" + UUID.randomUUID();
    }

    static final class Widget {
        final String name;

        Widget(final String name) {
            this.name = name;
        }
    }

    // registers a column mapper for Widget, which has no built-in mapper
    private static final Consumer<ConfigRegistry> WIDGET_MAPPER = config ->
            config.configure(ColumnMappers.class, m -> m.register(Widget.class, (rs, col, ctx) -> new Widget(rs.getString(col))));

    @Test
    public void openScopeAppliesConfig() {
        final Jdbi jdbi = Jdbi.create(url());
        try (Handle h = jdbi.open(WIDGET_MAPPER)) {
            assertThat(h.createQuery("select 'sprocket'").mapTo(Widget.class).one().name).isEqualTo("sprocket");
        }
    }

    @Test
    public void openScopeDoesNotAffectJdbi() {
        final Jdbi jdbi = Jdbi.create(url());
        try (Handle scoped = jdbi.open(WIDGET_MAPPER)) {
            assertThat(scoped.createQuery("select 'x'").mapTo(Widget.class).one().name).isEqualTo("x");
        }
        // a plain handle from the same Jdbi never saw the scoped mapper
        try (Handle plain = jdbi.open()) {
            assertThatThrownBy(() -> plain.createQuery("select 'x'").mapTo(Widget.class).one())
                    .isInstanceOf(NoSuchMapperException.class);
        }
    }

    @Test
    public void withHandleScopeAppliesConfig() {
        final Jdbi jdbi = Jdbi.create(url());
        final String name = jdbi.withHandle(WIDGET_MAPPER,
                h -> h.createQuery("select 'gadget'").mapTo(Widget.class).one().name);
        assertThat(name).isEqualTo("gadget");
    }

    @Test
    public void inTransactionScopeAppliesConfig() {
        final Jdbi jdbi = Jdbi.create(url());
        final String name = jdbi.inTransaction(WIDGET_MAPPER,
                h -> h.createQuery("select 'cog'").mapTo(Widget.class).one().name);
        assertThat(name).isEqualTo("cog");
    }

    @Test
    public void scopedHandleListenerSeesFullLifecycle() {
        final Jdbi jdbi = Jdbi.create(url());
        final AtomicInteger created = new AtomicInteger();
        final AtomicInteger closed = new AtomicInteger();
        // the scope is applied before the handle copies its listeners, so a handleCreated listener is honored
        final Consumer<ConfigRegistry> listenerScope = config -> config.configure(Handles.class, h -> h.addListener(new HandleListener() {
            @Override
            public void handleCreated(final Handle handle) {
                created.incrementAndGet();
            }

            @Override
            public void handleClosed(final Handle handle) {
                closed.incrementAndGet();
            }
        }));

        try (Handle h = jdbi.open(listenerScope)) {
            assertThat(created).hasValue(1);
        }
        assertThat(closed).hasValue(1);
    }

    @Test
    public void nullScopeRejected() {
        final Jdbi jdbi = Jdbi.create(url());
        assertThatThrownBy(() -> jdbi.open((Consumer<ConfigRegistry>) null)).isInstanceOf(NullPointerException.class);
    }
}
