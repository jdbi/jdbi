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

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdbi.v3.core.argument.internal.ObjectPropertyNamedArgumentFinder;
import org.jdbi.v3.core.argument.internal.TypedValue;
import org.jdbi.v3.core.config.JdbiCache;
import org.jdbi.v3.core.config.JdbiCaches;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;

/**
 * Inspect an object and binds parameters based on each of its public fields.
 */
public class ObjectFieldArguments extends ObjectPropertyNamedArgumentFinder {
    private static final JdbiCache<Class<?>, Map<String, Field>> FIELD_CACHE =
            JdbiCaches.declare(beanClass ->
                Stream.of(beanClass.getFields())
                    .collect(Collectors.toMap(Field::getName, Function.identity())));
    private final Class<?> beanClass;

    /**
     * @param prefix an optional prefix (we insert a '.' as a separator)
     * @param bean the bean to inspect and bind
     */
    public ObjectFieldArguments(String prefix, Object bean) {
        super(prefix, bean);

        this.beanClass = bean.getClass();
    }

    @Override
    protected Optional<TypedValue> getValue(String name, StatementContext ctx) {
        Field field = FIELD_CACHE.get(beanClass, ctx).get(name);

        if (field == null) {
            return Optional.empty();
        }

        try {
            QualifiedType<?> type = QualifiedType.of(field.getGenericType())
                                    .withAnnotations(ctx.getConfig(Qualifiers.class).findFor(field));
            Object value = field.get(obj);

            return Optional.of(new TypedValue(type, value));
        } catch (IllegalAccessException e) {
            throw new UnableToCreateStatementException(String.format("Access exception getting field for "
                    + "bean property [%s] on [%s]",
                name, obj), e, ctx);
        }
    }

    @Override
    protected NamedArgumentFinder getNestedArgumentFinder(Object obj) {
        return new ObjectFieldArguments(null, obj);
    }

    @Override
    public String toString() {
        return "{lazy bean field arguments \"" + obj + "\"";
    }
}

