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
package org.jdbi.v3.core.argument;

import java.lang.reflect.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.jdbi.v3.core.StatementContext;

public class SqlArrayArgument<T> implements Argument {

    private final String typeName;
    private final Object[] array;

    public SqlArrayArgument(ArrayElementMapper<T> elementMapper, Object newArray) {
        this.typeName = elementMapper.getTypeName();
        int length = Array.getLength(newArray);
        this.array = new Object[length];
        for (int i = 0; i < length; i++) {
            array[i] = elementMapper.mapArrayElement((T) Array.get(newArray, i));
        }
    }

    public SqlArrayArgument(ArrayElementMapper<T> elementMapper, List<T> list) {
        this.typeName = elementMapper.getTypeName();
        this.array = list.stream().map(elementMapper::mapArrayElement).toArray();
    }

    @Override
    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
        java.sql.Array sqlArray = statement.getConnection().createArrayOf(typeName, array);
        statement.setArray(position, sqlArray);
        // TODO register cleanable to close array after statement execution
    }
}
