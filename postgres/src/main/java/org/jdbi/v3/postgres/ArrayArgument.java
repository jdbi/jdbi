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
package org.jdbi.v3.postgres;

import java.lang.reflect.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.argument.Argument;

public class ArrayArgument implements Argument {

    private final String elementType;
    private final Object[] array;

    private ArrayArgument(String elementType, Object newArray) {
        this.elementType = elementType;
        int length = Array.getLength(newArray);
        // We can't cast newArray to Object[], so we need to make a new array
        // of Object[] and copy newArray's content to it
        this.array = new Object[length];
        for (int i = 0; i < length; i++) {
            array[i] = Array.get(newArray, i);
        }
    }

    public static ArrayArgument fromArray(String elementType, Object[] array) {
        return fromAnyArray(elementType, array);
    }

    public static ArrayArgument fromAnyArray(String elementType, Object array) {
        return new ArrayArgument(elementType, array);
    }

    public static ArrayArgument fromVarargs(String elementType, Object... elements) {
        return fromArray(elementType, elements);
    }

    @Override
    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
        java.sql.Array sqlArray = statement.getConnection().createArrayOf(elementType, array);
        statement.setArray(position, sqlArray);
    }
}
