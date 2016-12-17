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

import java.lang.reflect.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.util.ReflectionArrayIterator;

class SqlArrayArgument<T> implements Argument {

    private final String typeName;
    private final Object[] array;

    @SuppressWarnings("unchecked")
    SqlArrayArgument(SqlArrayType<T> arrayType, Object newArray) {
        this.typeName = arrayType.getTypeName();

        List<Object> elements = new ArrayList<>(
                newArray.getClass().isArray() ? Array.getLength(newArray) : 10);
        ReflectionArrayIterator.of(newArray).forEachRemaining(
                e -> elements.add(arrayType.convertArrayElement((T) e)));
        array = elements.toArray();
    }

    SqlArrayArgument(SqlArrayType<T> arrayType, Collection<T> list) {
        this.typeName = arrayType.getTypeName();
        this.array = list.stream().map(arrayType::convertArrayElement).toArray();
    }

    @Override
    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
        SqlArrayArgumentStrategy argumentStyle = ctx.getSqlArrayArgumentStrategy();
        switch(argumentStyle) {
            case SQL_ARRAY:
                java.sql.Array sqlArray = statement.getConnection().createArrayOf(typeName, array);
                ctx.addCleanable(sqlArray::free);
                statement.setArray(position, sqlArray);
                break;
            case OBJECT_ARRAY:
                statement.setObject(position, array);
                break;
            default:
                throw new UnsupportedOperationException("Unknown array argument style " + argumentStyle);
        }
    }
}
