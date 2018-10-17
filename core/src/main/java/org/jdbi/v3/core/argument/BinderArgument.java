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
import org.jdbi.v3.core.statement.StatementContext;

final class BinderArgument<T> implements Argument {
    private final T value;
    private final Class<T> klass;
    private final int type;
    private final StatementBinder<T> binder;

    BinderArgument(Class<T> klass, int type, StatementBinder<T> binder, T value) {
        this.binder = binder;
        this.klass = klass;
        this.type = type;
        this.value = value;
    }

    @Override
    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
        if (value == null) {
            statement.setNull(position, type);
            return;
        }
        binder.bind(statement, position, value);
    }

    @Override
    public String toString() {
        if (klass.isArray()) {
            return String.format("{array of %s length %s}", klass.getComponentType(), Array.getLength(value));
        }
        return String.valueOf(value);
    }

    static <T> ArgBuilder<T> builder(Class<T> klass, int sqlType, StatementBinder<T> binder) {
        return value -> new BinderArgument<>(klass, sqlType, binder, value);
    }
}
