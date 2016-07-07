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

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import org.jdbi.v3.core.StatementContext;

public class ObjectArgumentFactory implements ArgumentFactory
{
    public static ArgumentFactory create(Class<?> type) {
        return create(type, null);
    }

    public static ArgumentFactory create(Class<?> type, Integer sqlType) {
        return new ObjectArgumentFactory(type, sqlType);
    }

    private final Class<?> type;
    private final Integer sqlType;

    private ObjectArgumentFactory(Class<?> type, Integer sqlType) {
        this.type = type;
        this.sqlType = sqlType;
    }

    @Override
    public Optional<Argument> build(Type expectedType, Object value, StatementContext ctx) {
        return Objects.equals(type, expectedType) || type.isInstance(value)
                ? Optional.of(new ObjectArgument(value, sqlType))
                : Optional.empty();
    }
}
