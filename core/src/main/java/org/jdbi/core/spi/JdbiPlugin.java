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
     * {@link Jdbi.Builder#build()} for each installed plugin, in install order, before {@link #customizeJdbi(Jdbi)}.
     * It is the preferred hook for plugins that only add configuration (mappers, arguments, and the like), because
     * it runs while the {@code Jdbi} is still being assembled rather than mutating it after construction.
     *
     * @param builder the builder to contribute to
     */
    default void configure(Jdbi.Builder builder) {}

    /**
     * Configure customizations global to any object managed by this Jdbi.
     * This method is invoked immediately when the plugin is installed.
     * @param jdbi the jdbi to customize
     * @throws SQLException something went wrong with the database
     * @deprecated contribute configuration from {@link #configure(Jdbi.Builder)} instead; this hook is applied after
     *             the {@code Jdbi} is constructed and is going away.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    default void customizeJdbi(Jdbi jdbi) throws SQLException {}

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
