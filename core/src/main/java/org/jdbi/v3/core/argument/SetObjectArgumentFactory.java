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
package org.jdbi.v3.core.argument;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.v3.core.config.ConfigRegistry;

/**
 * Factory that uses {@link java.sql.PreparedStatement#setObject(int, Object, int)} to bind values.
 */
public class SetObjectArgumentFactory implements ArgumentFactory.Preparable {

    private final Map<Class<?>, Integer> supportedTypes;

    protected SetObjectArgumentFactory(Map<Class<?>, Integer> types) {
        supportedTypes = types;
    }

    /**
     * Creates a new {@link ArgumentFactory} that maps objects to {@link java.sql.Types} values.
     *
     * @param types the Java {@link Class}es to handle with the {@link java.sql.Types} they bind to.
     * @return an {@link ArgumentFactory} that handles only the given {@link Class}es.
     */
    public static ArgumentFactory forClasses(Map<Class<?>, Integer> types) {
        return new SetObjectArgumentFactory(new HashMap<>(types));
    }

    @Override
    public Optional<Function<Object, Argument>> prepare(Type type, ConfigRegistry config) {
        return Optional.of(type)
            .filter(Class.class::isInstance)
            .map(Class.class::cast)
            .map(supportedTypes::get)
            .map(sqlType -> value -> ObjectArgument.of(value, sqlType));
    }

    /**
     * @deprecated no longer used
     */
    @Override
    @Deprecated
    public Collection<? extends Type> prePreparedTypes() {
        return supportedTypes.keySet();
    }
}
