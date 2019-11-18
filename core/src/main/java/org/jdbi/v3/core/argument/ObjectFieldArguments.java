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

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdbi.v3.core.argument.internal.ObjectPropertyNamedArgumentFinder;
import org.jdbi.v3.core.argument.internal.TypedValue;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiCache;
import org.jdbi.v3.core.config.JdbiCaches;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * Inspect an object and binds parameters based on each of its public fields.
 * @deprecated this functionality will remain supported, but this class should not be API
 */
@Deprecated
public class ObjectFieldArguments extends ObjectPropertyNamedArgumentFinder {
    private static final JdbiCache<Class<?>, Map<String, Function<Object, TypedValue>>> FIELD_CACHE =
            JdbiCaches.declare((config, beanClass) ->
                Stream.of(beanClass.getFields())
                    .collect(Collectors.toMap(Field::getName, f -> {
                        QualifiedType<?> qualifiedType = QualifiedType.of(f.getType())
                                .withAnnotations(config.get(Qualifiers.class).findFor(f));
                        Function<Object, Object> getter = Unchecked.function(
                                Unchecked.function(MethodHandles.lookup()::unreflectGetter).apply(f)::invoke);
                        return obj -> new TypedValue(qualifiedType, getter.apply(obj));
                    })));
    private final Class<?> beanClass;

    /**
     * @param prefix an optional prefix (we insert a '.' as a separator)
     * @param bean the bean to inspect and bind
     */
    public ObjectFieldArguments(String prefix, Object bean) {
        super(prefix, bean);
        this.beanClass = bean.getClass();
    }

    public Optional<Function<Object, TypedValue>> getter(String name, ConfigRegistry config) {
        return Optional.ofNullable(FIELD_CACHE.get(beanClass, config).get(name));
    }

    @Override
    protected Optional<TypedValue> getValue(String name, StatementContext ctx) {
        return getter(name, ctx.getConfig())
                .map(getter -> getter.apply(obj));
    }

    @Override
    protected NamedArgumentFinder getNestedArgumentFinder(Object source) {
        return new ObjectFieldArguments(null, source);
    }

    @Override
    public String toString() {
        return "{lazy bean field arguments \"" + obj + "\"";
    }
}

