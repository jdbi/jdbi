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

import java.lang.reflect.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

import org.jdbi.v3.core.SqlArrayConfiguration.ArgumentStyle;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.SqlArrayType;

class SqlArrayArgument<T> implements Argument {

    private final String typeName;
    private final Object[] array;

    @SuppressWarnings("unchecked")
    SqlArrayArgument(SqlArrayType<T> arrayType, Object newArray) {
        this.typeName = arrayType.getTypeName();
        int length = Array.getLength(newArray);
        this.array = new Object[length];
        for (int i = 0; i < length; i++) {
            array[i] = arrayType.convertArrayElement((T) Array.get(newArray, i));
        }
    }

    SqlArrayArgument(SqlArrayType<T> arrayType, Collection<T> list) {
        this.typeName = arrayType.getTypeName();
        this.array = list.stream().map(arrayType::convertArrayElement).toArray();
    }

    @Override
    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
        java.sql.Array sqlArray = statement.getConnection().createArrayOf(typeName, array);
        ctx.getCleanables().add(sqlArray::free);
        statement.setArray(position, sqlArray);
    }
}
