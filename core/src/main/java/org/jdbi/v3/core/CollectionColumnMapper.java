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
package org.jdbi.v3.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.function.Supplier;

import org.jdbi.v3.core.mapper.ColumnMapper;

class CollectionColumnMapper<T> implements ColumnMapper<Collection<T>> {
    private final ColumnMapper<T> elementMapper;
    private final Supplier<Collection<T>> collectionSupplier;

    CollectionColumnMapper(ColumnMapper<T> elementMapper,
                     Supplier<Collection<T>> collectionSupplier) {
        this.elementMapper = elementMapper;
        this.collectionSupplier = collectionSupplier;
    }

    @Override
    public Collection<T> map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        java.sql.Array array = r.getArray(columnNumber);
        try {
            return buildFromResultSet(array, ctx);
        }
        finally {
            array.free();
        }
    }

    private Collection<T> buildFromResultSet(java.sql.Array array, StatementContext ctx) throws SQLException {
        Collection<T> result = collectionSupplier.get();

        try (ResultSet rs = array.getResultSet()) {
            while (rs.next()) {
                result.add(elementMapper.map(rs, 2, ctx));
            }
        }

        return result;
    }
}
