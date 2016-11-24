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
import java.util.Collection;

import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.argument.Argument;

class SqlArrayArgument<T> implements Argument<Object> {

    private final SqlArrayType<T> arrayType;

    @SuppressWarnings("unchecked")
    SqlArrayArgument(SqlArrayType<T> arrayType) {
        this.arrayType = arrayType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void apply(PreparedStatement statement, int position, Object value, StatementContext ctx) throws SQLException {
        int length = Array.getLength(value);
        Object[] array = new Object[length];
        for (int i = 0; i < length; i++) {
            array[i] = arrayType.convertArrayElement((T) Array.get(value, i));
        }

        SqlArrayArgumentStrategy argumentStyle = ctx.getSqlArrayArgumentStrategy();
        switch(argumentStyle) {
            case SQL_ARRAY:
                java.sql.Array sqlArray = statement.getConnection().createArrayOf(arrayType.getTypeName(), array);
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
