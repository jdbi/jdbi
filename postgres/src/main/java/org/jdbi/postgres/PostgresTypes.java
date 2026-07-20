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
package org.jdbi.postgres;

import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.internal.exceptions.Unchecked;
import org.postgresql.PGConnection;
import org.postgresql.util.PGobject;

/**
 * Handler for PostgreSQL custom types.
 * <p>
 * This configuration is immutable: {@link #registerCustomType} and {@link #lobApi} return a new instance,
 * leaving the receiver unchanged. Array binding for registered custom types is provided by a single
 * {@link PostgresCustomTypeArrayFactory} that {@link PostgresPlugin} installs and that consults this
 * configuration, so registration is a pure update here and no longer needs a reference back to the
 * configuration registry.
 */
public final class PostgresTypes implements JdbiConfig<PostgresTypes> {
    private final Map<Class<? extends PGobject>, String> types;

    public PostgresTypes() {
        this(Map.of());
    }

    private PostgresTypes(final Map<Class<? extends PGobject>, String> types) {
        this.types = types;
    }

    /**
     * Register a Postgres custom type.
     * @param clazz the class implementing the Java representation of the custom type;
     * must extend {@link PGobject}.
     * @param typeName the Postgres custom type name
     * @return a copy of this configuration with the custom type registered
     */
    @CheckReturnValue
    public PostgresTypes registerCustomType(Class<? extends PGobject> clazz, String typeName) {
        final Map<Class<? extends PGobject>, String> updated = new HashMap<>(types);
        updated.put(clazz, typeName);
        return new PostgresTypes(Map.copyOf(updated));
    }

    /**
     * Returns the registered Postgres type name for the given custom-type element class, or {@code null} if the
     * class is not a registered custom type. Consulted by {@link PostgresCustomTypeArrayFactory}.
     */
    String sqlArrayTypeName(Class<?> elementType) {
        return types.get(elementType);
    }

    /**
     * Add handler for each registered PostgreSQL custom type
     *
     * @param connection connection on which to add all registered PostgreSQL custom types
     * @return this configuration, unchanged
     */
    PostgresTypes addTypesToConnection(PGConnection connection) {
        types.forEach((clazz, type) -> Unchecked.<String, Class<? extends PGobject>>biConsumer(connection::addDataType).accept(type, clazz));
        return this;
    }

    @Override
    public PostgresTypes createCopy() {
        // Immutable: safe to share across registries.
        return this;
    }
}
