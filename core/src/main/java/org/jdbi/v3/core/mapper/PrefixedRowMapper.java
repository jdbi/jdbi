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
package org.jdbi.v3.core.mapper;

import org.jdbi.v3.meta.Alpha;

/**
 * A {@link RowMapper} that reads its values from columns with a common name prefix.
 * <p>
 * Mappers that implement this interface take part in prefixed mapper lookup, see
 * {@link RowMappers#findFor(java.lang.reflect.Type, String)} and
 * {@link org.jdbi.v3.core.result.RowView#getRow(Class, String)}. This makes it possible to
 * register multiple mappers for the same type, each with a different prefix, and select
 * between them per call.
 * <p>
 * The reflective mappers ({@code BeanMapper}, {@code ConstructorMapper}, {@code FieldMapper})
 * implement this interface.
 *
 * @param <T> the mapped type
 */
@Alpha
public interface PrefixedRowMapper<T> extends RowMapper<T> {

    /**
     * Returns the column name prefix this mapper reads its values from.
     * A mapper created without a prefix declares the empty string.
     *
     * @return the declared column name prefix, never null
     */
    String getPrefix();
}
