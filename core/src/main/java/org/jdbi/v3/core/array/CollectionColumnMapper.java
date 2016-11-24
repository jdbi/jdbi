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
package org.jdbi.v3.core.array;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.function.Supplier;

import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.mapper.ColumnMapper;

class CollectionColumnMapper<T, C extends Collection<T>> implements ColumnMapper<C> {
    private final ColumnMapper<T> elementMapper;
    private final Supplier<C> collectionSupplier;

    CollectionColumnMapper(ColumnMapper<T> elementMapper,
                           Supplier<C> collectionSupplier) {
        this.elementMapper = elementMapper;
        this.collectionSupplier = collectionSupplier;
    }

    @Override
    public C map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        java.sql.Array array = r.getArray(columnNumber);
        try {
            return buildFromResultSet(array, ctx);
        }
        finally {
            array.free();
        }
    }

    private C buildFromResultSet(java.sql.Array array, StatementContext ctx) throws SQLException {
        C result = collectionSupplier.get();

        try (ResultSet rs = array.getResultSet()) {
            while (rs.next()) {
                result.add(elementMapper.map(rs, 2, ctx));
            }
        }

        return result;
    }
}
