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

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.extension.ExtensionHandler;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for jdbi/jdbi#2991: the shared {@code ExtensionMetadata} cache must not serve metadata
 * computed before a metadata-relevant configuration change. {@code Handlers} and {@code HandlerDecorators}
 * feed metadata computation but live outside {@code Extensions}, so their {@code register} methods invalidate
 * the shared cache (via {@code Extensions.invalidateMetadataCache()}) exactly as the {@code Extensions}
 * registrations do. Without that invalidation, a handle that attaches a type after a sibling handle warmed the
 * shared cache would receive stale metadata computed without the newly registered handler.
 *
 * <p>A {@link HandlerFactory}'s {@code buildHandler} is invoked while metadata is computed for a type, so a
 * counting factory reveals whether a given attach recomputed metadata or reused a cached value.
 */
public class TestHandlerRegistrationAfterAttach {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin());

    public interface WarmDao extends SqlObject {
        default String warm() {
            return "warm";
        }
    }

    // Counts how many times metadata is computed for WarmDao (buildHandler is consulted per method during
    // metadata computation). Returns empty so it never actually supplies a handler, leaving stock resolution
    // of the default method intact.
    static final class CountingHandlerFactory implements HandlerFactory {
        final AtomicInteger buildHandlerCalls = new AtomicInteger();

        @Override
        public Optional<Handler> buildHandler(Class<?> sqlObjectType, Method method) {
            if (sqlObjectType == WarmDao.class) {
                buildHandlerCalls.incrementAndGet();
            }
            return Optional.empty();
        }
    }

    @Test
    public void handlerRegisteredAfterSiblingAttachForcesRecompute() {
        Jdbi jdbi = h2Extension.getJdbi();
        CountingHandlerFactory counter = new CountingHandlerFactory();
        jdbi.configure(Handlers.class, c -> c.register(counter));

        // Warm the shared metadata cache for WarmDao from one handle.
        try (Handle warm = jdbi.open()) {
            warm.attach(WarmDao.class);
        }
        int afterWarm = counter.buildHandlerCalls.get();
        assertThat(afterWarm).as("warming computes WarmDao metadata once").isPositive();

        // A sibling handle attaching the same type reuses the shared cache: no recomputation.
        try (Handle cached = jdbi.open()) {
            cached.attach(WarmDao.class);
        }
        assertThat(counter.buildHandlerCalls)
                .as("a sibling handle hits the shared cache without recomputing")
                .hasValue(afterWarm);

        // Register another handler factory imperatively AFTER the cache was warmed. This must invalidate the
        // shared cache so that WarmDao's metadata is recomputed against the new configuration on the next attach.
        jdbi.configure(Handlers.class, c -> c.register(new CountingHandlerFactory()));

        try (Handle afterRegister = jdbi.open()) {
            afterRegister.attach(WarmDao.class);
        }
        assertThat(counter.buildHandlerCalls.get())
                .as("registering a handler factory after warming forces metadata recomputation (#2991)")
                .isGreaterThan(afterWarm);
    }

    // Counts how many times the decorator is consulted while metadata is built for WarmDao, without changing
    // the handler it decorates. customize() is the method invoked at metadata-build time (decorateHandler is
    // only reached for handlers that are sqlobject Handler instances), so counting here is build-accurate.
    static final class CountingHandlerDecorator implements HandlerDecorator {
        final AtomicInteger decorateCalls = new AtomicInteger();

        @Override
        public ExtensionHandler customize(ExtensionHandler defaultHandler, Class<?> extensionType, Method method) {
            if (extensionType == WarmDao.class) {
                decorateCalls.incrementAndGet();
            }
            return defaultHandler;
        }

        @Override
        public Handler decorateHandler(Handler base, Class<?> sqlObjectType, Method method) {
            return base;
        }
    }

    @Test
    public void decoratorRegisteredAfterSiblingAttachForcesRecompute() {
        Jdbi jdbi = h2Extension.getJdbi();
        CountingHandlerDecorator counter = new CountingHandlerDecorator();
        jdbi.configure(HandlerDecorators.class, c -> c.register(counter));

        try (Handle warm = jdbi.open()) {
            warm.attach(WarmDao.class);
        }
        int afterWarm = counter.decorateCalls.get();
        assertThat(afterWarm).as("warming computes WarmDao metadata once").isPositive();

        try (Handle cached = jdbi.open()) {
            cached.attach(WarmDao.class);
        }
        assertThat(counter.decorateCalls)
                .as("a sibling handle hits the shared cache without recomputing")
                .hasValue(afterWarm);

        // Registering a decorator after warming must invalidate the shared cache, exactly as a handler
        // factory does; decorators change handler behavior, so a stale decorator cache is user-visible.
        jdbi.configure(HandlerDecorators.class, c -> c.register(new CountingHandlerDecorator()));

        try (Handle afterRegister = jdbi.open()) {
            afterRegister.attach(WarmDao.class);
        }
        assertThat(counter.decorateCalls.get())
                .as("registering a handler decorator after warming forces metadata recomputation (#2991)")
                .isGreaterThan(afterWarm);
    }

    @Test
    public void handlerRegisteredOnHandleDoesNotLeakToSiblings() {
        Jdbi jdbi = h2Extension.getJdbi();
        CountingHandlerFactory rootCounter = new CountingHandlerFactory();
        jdbi.configure(Handlers.class, c -> c.register(rootCounter));

        // One handle registers a factory locally, then attaches WarmDao. Its local registration invalidates
        // only its own cache; the root cache stays empty.
        try (Handle local = jdbi.open()) {
            local.configure(Handlers.class, c -> c.register(new CountingHandlerFactory()));
            local.attach(WarmDao.class);
        }
        int afterLocal = rootCounter.buildHandlerCalls.get();

        // A sibling that never registered the local factory must not inherit its metadata from a shared
        // cache: the first sibling attach still computes WarmDao metadata from the root configuration.
        try (Handle sibling = jdbi.open()) {
            sibling.attach(WarmDao.class);
        }
        assertThat(rootCounter.buildHandlerCalls.get())
                .as("a sibling does not inherit metadata computed under a handle-local registration")
                .isGreaterThan(afterLocal);
    }
}
