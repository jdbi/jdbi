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
package org.jdbi.v3.postgres;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jdbi.v3.core.array.SqlArrayTypes;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.postgresql.PGConnection;
import org.postgresql.util.PGobject;

/**
 * Handler for PostgreSQL custom types.
 */
public class PostgresTypes implements JdbiConfig<PostgresTypes> {
    private final Map<Class<? extends PGobject>, String> types = new ConcurrentHashMap<>();
    private ConfigRegistry registry;

    @SuppressWarnings("unused")
    public PostgresTypes() {}

    private PostgresTypes(PostgresTypes that) {
        this.types.putAll(that.types);
    }

    @Override
    public void setRegistry(ConfigRegistry registry) {
        this.registry = registry;
    }

    /**
     * Register a Postgres custom type.
     * @param clazz the class implementing the Java representation of the custom type;
     * must extend {@link PGobject}.
     * @param typeName the Postgres custom type name
     */
    public PostgresTypes registerCustomType(Class<? extends PGobject> clazz, String typeName) {
        registry.get(SqlArrayTypes.class).register(clazz, typeName);

        types.put(clazz, typeName);

        return this;
    }

    /**
     * Add handler for each registered PostgreSQL custom type
     *
     * @param connection connection on which to add all registered PostgreSQL custom types
     */
    void addTypesToConnection(PGConnection connection) {
        types.forEach((clazz, type) -> Unchecked.<String, Class>biConsumer(connection::addDataType).accept(type, clazz));
    }

    @Override
    public PostgresTypes createCopy() {
        return new PostgresTypes(this);
    }
}
