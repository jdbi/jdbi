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

import io.vavr.control.Option;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.statement.StatementContext;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

public class VavrOptionMapper<T> implements ColumnMapper<Option<T>> {

    private final Type type;

    private VavrOptionMapper(Type type) {
        this.type = type;
    }

    public static ColumnMapper<?> of(Type type) {
        return new VavrOptionMapper(type);
    }

    public static ColumnMapperFactory factory() {
        return (type, config) -> {
            Class<?> rawType = getErasedType(type);
            if (rawType == Option.class) {
                return Optional.of(VavrOptionMapper.of(type));
            }
            return Optional.empty();
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public Option<T> map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        final ColumnMapper<?> mapper = ctx.findColumnMapperFor(
                GenericTypes.findGenericParameter(type, Option.class)
                        .orElseThrow(() -> new NoSuchMapperException("No mapper for raw Option type")))
                .orElseThrow(() -> new NoSuchMapperException("No mapper for type " + type + " nested in Option"));

        return (Option<T>) Option.of(mapper.map(r, columnNumber, ctx));
    }

}
