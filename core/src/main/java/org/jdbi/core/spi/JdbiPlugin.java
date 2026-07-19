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
package org.jdbi.core.spi;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Consumer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.config.ConfigRegistry;

/**
 * A plugin is given an opportunity to customize instances of various {@code Jdbi}
 * types before they are returned from their factories.
 */
public interface JdbiPlugin {
    /**
     * Returns a plugin whose {@link #configure(Jdbi.Builder)} applies the given consumer to the builder. This is a
     * lambda shorthand for bundling a bit of build-time configuration without declaring an anonymous {@code JdbiPlugin}:
     * <pre>{@code
     * builder.installPlugin(JdbiPlugin.of(b -> b.registerRowMapper(mapper).registerArgument(argument)));
     * }</pre>
     * The returned plugin implements only {@code configure(Builder)}; it does not customize handles or connections.
     *
     * @param configureConsumer applied to the {@link Jdbi.Builder} during assembly
     * @return a plugin that runs the given consumer at build time
     */
    static JdbiPlugin of(Consumer<Jdbi.Builder> configureConsumer) {
        Objects.requireNonNull(configureConsumer, "null configureConsumer");
        return new JdbiPlugin() {
            @Override
            public void configure(Jdbi.Builder builder) {
                configureConsumer.accept(builder);
            }
        };
    }

    /**
     * Contributes configuration and knobs to a {@link Jdbi.Builder} during assembly. This method is invoked by
     * {@link Jdbi.Builder#build()} for each installed plugin, in install order. It is the hook for plugins that add
     * configuration (mappers, arguments, and the like), running while the {@code Jdbi} is still being assembled.
     *
     * @param builder the builder to contribute to
     */
    default void configure(Jdbi.Builder builder) {}

    /**
     * Contributes per-connection configuration to a new handle's config while the handle is still being constructed.
     * This is the hook for configuration that can only be computed once a JDBC {@link Connection} is available (for
     * example, binding database types to the live connection), applied during construction rather than by mutating a
     * finished handle. It runs after any caller-supplied config scope and before the handle's extension context is
     * derived. Prefer this over {@link #customizeHandle(Handle)} for anything that modifies the handle's config.
     *
     * @param connection the JDBC connection backing the new handle
     * @param config the new handle's config, still open for modification during construction
     * @throws SQLException something went wrong with the database
     */
    default void customizeHandleConfig(Connection connection, ConfigRegistry config) throws SQLException {}

    /**
     * Configure customizations for a new Handle instance.
     * @param handle the handle just created
     * @return the transformed handle
     * @throws SQLException something went wrong with the database
     */
    default Handle customizeHandle(Handle handle) throws SQLException {
        return handle;
    }

    /**
     * Configure customizations for a newly acquired Connection.
     * @param conn the connection Jdbi acquired
     * @return the transformed connection to use
     * @throws SQLException something went wrong with the database
     */
    default Connection customizeConnection(Connection conn) throws SQLException {
        return conn;
    }

    /**
     * Abstract base class for single-install JdbiPlugins.
     */
    @SuppressWarnings("EqualsGetClass")
    @SuppressFBWarnings("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR")
    abstract class Singleton implements JdbiPlugin {
        @Override
        public boolean equals(Object obj) {
            return obj != null && getClass().equals(obj.getClass());
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }
}
