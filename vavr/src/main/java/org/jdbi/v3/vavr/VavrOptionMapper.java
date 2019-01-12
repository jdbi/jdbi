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
package org.jdbi.v3.vavr;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import io.vavr.control.Option;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.statement.StatementContext;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

class VavrOptionMapper<T> implements ColumnMapper<Option<T>> {

    private final Type nestedType;

    private VavrOptionMapper(Type nestedType) {
        this.nestedType = nestedType;
    }

    static ColumnMapperFactory factory() {
        return (type, config) -> {
            Class<?> rawType = getErasedType(type);
            if (rawType == Option.class) {
                final Type nestedType = GenericTypes.findGenericParameter(type, Option.class)
                        .orElseThrow(() -> new NoSuchMapperException("No mapper for raw Option type"));
                return Optional.of(new VavrOptionMapper<>(nestedType));
            }
            return Optional.empty();
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public Option<T> map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        final ColumnMapper<?> mapper = ctx.findColumnMapperFor(nestedType)
                .orElseThrow(() -> new NoSuchMapperException("No mapper for type " + nestedType + " nested in Option"));
        return (Option<T>) Option.of(mapper.map(r, columnNumber, ctx));
    }

}
