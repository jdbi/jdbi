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
package org.jdbi.v3.guava;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.util.GenericTypes;

/**
 * Provide mapper instances that map Guava types.
 */
public class GuavaMappers {

    /**
     * Returns a {@link ColumnMapperFactory} which maps Guava types.
     * <p>Supported types:</p>
     *
     * <ul>
     *     <li>{@link ImmutableList}</li>
     * </ul>
     * @return
     */
    public static ColumnMapperFactory columnFactory() {
        return new ColumnFactory();
    }

    static class ColumnFactory implements ColumnMapperFactory {
        @Override
        public Optional<ColumnMapper<?>> build(Type type, StatementContext ctx) {
            Class<?> erasedType = GenericTypes.getErasedType(type);

            if (ImmutableList.class.equals(erasedType)) {
                return GenericTypes.findGenericParameter(type, ImmutableList.class)
                        .flatMap(ctx::findColumnMapperFor)
                        .map(ImmutableListArrayColumnMapper::new);
            }

            return Optional.empty();
        }
    }

    static class ImmutableListArrayColumnMapper<T> implements ColumnMapper<ImmutableList<T>> {
        private final ColumnMapper<T> elementMapper;

        ImmutableListArrayColumnMapper(ColumnMapper<T> elementMapper) {
            this.elementMapper = elementMapper;
        }

        @Override
        public ImmutableList<T> map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            java.sql.Array array = r.getArray(columnNumber);
            try {
                return buildFromResultSet(array, ctx);
            } finally {
                array.free();
            }
        }

        private ImmutableList<T> buildFromResultSet(java.sql.Array array, StatementContext ctx) throws SQLException {
            ImmutableList.Builder<T> list = ImmutableList.builder();

            try (ResultSet rs = array.getResultSet()) {
                while (rs.next()) {
                    list.add(elementMapper.map(rs, 2, ctx));
                }
            }

            return list.build();
        }
    }
}
