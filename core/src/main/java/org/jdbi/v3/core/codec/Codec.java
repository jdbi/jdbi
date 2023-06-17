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
package org.jdbi.v3.core.codec;

import java.util.function.Function;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.meta.Beta;

/**
 * A Codec provides a convenient way for a bidirectional mapping of an attribute to a database column.
 * <p>
 * Groups a {@link ColumnMapper} and an {@link org.jdbi.v3.core.argument.Argument} mapping function for a given type.
 * <p>
 * Codec instances can replace existing Mapper and Argument pairs.
 */
@Beta
public interface Codec<T> {

    /**
     * Returns a {@link ColumnMapper} that creates an attribute value from a database column.
     * <p>
     * Either this method or {@link #getColumnMapper(ConfigRegistry)} must be implemented.
     */
    default ColumnMapper<T> getColumnMapper() {
        throw new UnsupportedOperationException("getColumnMapper");
    }

    /**
     * Returns a {@link ColumnMapper} that creates an attribute value from a database column.
     * <p>
     * This method is optional. If it is not implemented, the result of {@link #getColumnMapper()} is returned.
     *
     * @param configRegistry The {@link ConfigRegistry} that this argument belongs to.
     */
    default ColumnMapper<T> getColumnMapper(ConfigRegistry configRegistry) {
        return getColumnMapper();
    }

    /**
     * Returns a {@link Function} that creates an {@link Argument} to map an attribute value onto the database column.
     * <p>
     * Either this method or {@link #getArgumentFunction(ConfigRegistry)} must be implemented.
     */
    default Function<T, Argument> getArgumentFunction() {
        throw new UnsupportedOperationException("getArgumentFunction");
    }

    /**
     * Returns a {@link Function} that creates an {@link Argument} to map an attribute value onto the database column.
     * <p>
     * This method is optional. If it is not implemented, the result of {@link #getArgumentFunction()} is returned.
     *
     * @param configRegistry The {@link ConfigRegistry} that this argument belongs to.
     */
    default Function<T, Argument> getArgumentFunction(ConfigRegistry configRegistry) {
        return getArgumentFunction();
    }
}
