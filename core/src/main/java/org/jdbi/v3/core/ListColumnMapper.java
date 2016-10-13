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
import java.util.List;
import java.util.function.Supplier;

import org.jdbi.v3.core.mapper.ColumnMapper;

class ListColumnMapper<T> implements ColumnMapper<List<T>> {
    private final ColumnMapper<T> elementMapper;
    private final Supplier<List<T>> listSupplier;

    ListColumnMapper(ColumnMapper<T> elementMapper,
                     Supplier<List<T>> listSupplier) {
        this.elementMapper = elementMapper;
        this.listSupplier = listSupplier;
    }

    @Override
    public List<T> map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        java.sql.Array array = r.getArray(columnNumber);
        try {
            return buildFromResultSet(array, ctx);
        } finally {
            array.free();
        }
    }

    private List<T> buildFromResultSet(java.sql.Array array, StatementContext ctx) throws SQLException {
        List<T> list = listSupplier.get();

        try (ResultSet rs = array.getResultSet()) {
            while (rs.next()) {
                list.add(elementMapper.map(rs, 2, ctx));
            }
        }

        return list;
    }
}
