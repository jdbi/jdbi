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
package org.jdbi.v3.core.extension;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for jdbi/jdbi#2991.
 *
 * <p>Each {@link Handle} receives a copy of the root {@link org.jdbi.v3.core.config.ConfigRegistry}.
 * The {@link Extensions} metadata cache is shared across those copies, so metadata computed inside a
 * handle obtained via {@code handle.attach()} is visible to every other handle forked from the same root.
 * Before the fix, the cache was deep-copied per handle, so an {@code attach()}-only workload recomputed
 * {@link ExtensionMetadata} on every fresh handle.
 *
 * <p>{@link ExtensionFactory#buildExtensionMetadata} is invoked exactly once per metadata computation
 * (see {@code Extensions.createMetadata}), so counting its invocations counts cache misses without
 * touching production code.
 */
public class TestExtensionMetadataCaching {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    public interface CountingExtension {
        default String ping() {
            return "pong";
        }
    }

    static class CountingExtensionFactory implements ExtensionFactory {

        final AtomicInteger metadataComputations = new AtomicInteger();

        @Override
        public boolean accepts(Class<?> extensionType) {
            return CountingExtension.class.equals(extensionType);
        }

        @Override
        public Set<FactoryFlag> getFactoryFlags() {
            return EnumSet.noneOf(FactoryFlag.class);
        }

        @Override
        public void buildExtensionMetadata(ExtensionMetadata.Builder builder) {
            metadataComputations.incrementAndGet();
        }
    }

    @Test
    public void attachOnlyComputesMetadataOnce() {
        CountingExtensionFactory factory = new CountingExtensionFactory();
        Jdbi jdbi = h2Extension.getJdbi().registerExtension(factory);

        for (int i = 0; i < 5; i++) {
            try (Handle handle = jdbi.open()) {
                handle.attach(CountingExtension.class);
            }
        }

        // The metadata cache is shared across handles forked from the same root, so the first attach
        // computes metadata and every later handle hits the shared cache (#2991).
        assertThat(factory.metadataComputations)
                .as("attach()-only workloads compute ExtensionMetadata once across all handles (#2991)")
                .hasValue(1);
    }

    @Test
    public void repeatedAttachOnSameHandleUsesHandleLocalCache() {
        CountingExtensionFactory factory = new CountingExtensionFactory();
        Jdbi jdbi = h2Extension.getJdbi().registerExtension(factory);

        try (Handle handle = jdbi.open()) {
            handle.attach(CountingExtension.class);
            handle.attach(CountingExtension.class);
            handle.attach(CountingExtension.class);
        }

        // Within a single handle the forked cache does its job.
        assertThat(factory.metadataComputations)
                .as("repeated attach on one handle hits the handle-local cache")
                .hasValue(1);
    }

    @Test
    public void primingRootCacheAvoidsPerHandleRecomputation() {
        CountingExtensionFactory factory = new CountingExtensionFactory();
        Jdbi jdbi = h2Extension.getJdbi().registerExtension(factory);

        // The documented workaround: prime the root config's cache before opening per-request handles.
        jdbi.withExtension(CountingExtension.class, CountingExtension::ping);
        int primed = factory.metadataComputations.get();
        assertThat(primed).isEqualTo(1);

        for (int i = 0; i < 5; i++) {
            try (Handle handle = jdbi.open()) {
                handle.attach(CountingExtension.class);
            }
        }

        // Forked handles inherit the pre-populated cache, so no further computation occurs.
        assertThat(factory.metadataComputations)
                .as("handles forked after priming inherit the warm root cache")
                .hasValue(1);
    }

    @Test
    public void reconfiguringACopyDoesNotShareMetadataWithSiblings() {
        CountingExtensionFactory rootFactory = new CountingExtensionFactory();
        Jdbi jdbi = h2Extension.getJdbi().registerExtension(rootFactory);

        // A copy that registers an extension of its own must not read metadata from, or write metadata to,
        // the shared cache: its metadata is computed from a different set of factories. Registering
        // invalidates the copy's cache, so it computes against its own configuration and the root cache is
        // untouched.
        CountingExtensionFactory copyFactory = new CountingExtensionFactory();
        try (Handle reconfigured = jdbi.open()) {
            reconfigured.registerExtension(copyFactory);
            reconfigured.attach(CountingExtension.class);
        }
        // The copy's own factory (registered at the front) handled the attach and computed metadata once.
        assertThat(copyFactory.metadataComputations)
                .as("a reconfigured copy computes metadata against its own configuration")
                .hasValue(1);
        // The root factory was never consulted, so the shared cache holds no entry for the copy's metadata.
        assertThat(rootFactory.metadataComputations)
                .as("reconfiguring a copy does not pollute the shared root cache")
                .hasValue(0);

        // Plain handles forked from the untouched root share the root cache: the first computes, the rest hit.
        for (int i = 0; i < 3; i++) {
            try (Handle plain = jdbi.open()) {
                plain.attach(CountingExtension.class);
            }
        }
        assertThat(rootFactory.metadataComputations)
                .as("the root cache is uncorrupted and shared across plain handles after a sibling reconfigured")
                .hasValue(1);
    }
}
