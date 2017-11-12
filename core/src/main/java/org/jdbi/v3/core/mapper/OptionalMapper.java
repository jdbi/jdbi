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

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.statement.StatementContext;

class OptionalMapper<T> implements ColumnMapper<Optional<T>> {

    private final Type type;

    private OptionalMapper(Type type) {
        this.type = type;
    }

    public static ColumnMapper<?> of(Type type) {
        return new OptionalMapper<>(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<T> map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        final ColumnMapper<?> mapper = ctx.findColumnMapperFor(
                GenericTypes.findGenericParameter(type, Optional.class)
                    .orElseThrow(() -> new NoSuchMapperException("No mapper for raw Optional type")))
                .orElseThrow(() -> new NoSuchMapperException("No mapper for type " + type + " nested in Optional"));
        return (Optional<T>) Optional.ofNullable(mapper.map(r, columnNumber, ctx));
    }
}
