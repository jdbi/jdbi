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

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Optional;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.util.GenericTypes;

class SqlArrayArgumentFactory implements ArgumentFactory {
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Optional<Argument> build(Type type, Object value, StatementContext ctx) {
        Class<?> erasedType = GenericTypes.getErasedType(type);
        if (erasedType.isArray()) {
            Class<?> elementType = erasedType.getComponentType();
            return ctx.findArrayTypeFor(elementType)
                    .map(arrayType -> new SqlArrayArgument<>(arrayType, value));
        }
        if (Collection.class.isAssignableFrom(erasedType)) {
            return guessElementType(type, (Collection<?>) value)
                    .flatMap(ctx::findArrayTypeFor)
                    .map(arrayType -> new SqlArrayArgument<>(arrayType, (Collection) value));
        }
        return Optional.empty();
    }

    private Optional<? extends Type> guessElementType(Type type, Collection<?> value) {
        Optional<? extends Type> result = GenericTypes.findGenericParameter(type, Collection.class);
        if (result.isPresent()) {
            return result;
        }
        result = value.stream().map(Object::getClass).findFirst();
        if (result.isPresent()) {
            return result;
        }
        return Optional.of(Object.class);
    }
}
