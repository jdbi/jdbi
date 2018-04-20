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

import static java.util.stream.Collectors.toMap;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;

/**
 * Inspect an object and binds parameters based on each of its public fields.
 */
public class ObjectFieldArguments extends ObjectPropertyNamedArgumentFinder
{
    private static final Map<Class<?>, Map<String, Field>> CLASS_FIELDS = ExpiringMap.builder()
        .expiration(10, TimeUnit.MINUTES)
        .expirationPolicy(ExpirationPolicy.ACCESSED)
        .entryLoader((Class<?> type) ->
            Stream.of(type.getFields())
                .collect(toMap(Field::getName, Function.identity())))
        .build();

    private final Map<String, Field> fields;

    /**
     * @param prefix an optional prefix (we insert a '.' as a separator)
     * @param bean the bean to inspect and bind
     */
    public ObjectFieldArguments(String prefix, Object bean) {
        super(prefix, bean);

        this.fields = CLASS_FIELDS.get(bean.getClass());
    }

    @Override
    Optional<TypedValue> getValue(String name, StatementContext ctx) {
        Field field = fields.get(name);

        if (field == null) {
            return Optional.empty();
        }

        try
        {
            Type type = field.getGenericType();
            Object value = field.get(object);

            return Optional.of(new TypedValue(type, value));
        }
        catch (IllegalAccessException e)
        {
            throw new UnableToCreateStatementException(String.format("Access exception getting field for " +
                    "bean property [%s] on [%s]",
                name, object), e, ctx);
        }
    }

    @Override
    NamedArgumentFinder getNestedArgumentFinder(Object obj) {
        return new ObjectFieldArguments(null, obj);
    }

    @Override
    public String toString() {
        return "{lazy bean field arguments \"" + object + "\"";
    }
}

