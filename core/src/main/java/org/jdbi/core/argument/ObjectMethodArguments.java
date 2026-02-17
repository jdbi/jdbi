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
package org.jdbi.core.argument;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jdbi.core.annotation.internal.JdbiAnnotations;
import org.jdbi.core.argument.internal.ObjectPropertyNamedArgumentFinder;
import org.jdbi.core.argument.internal.TypedValue;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.config.internal.ConfigCache;
import org.jdbi.core.config.internal.ConfigCaches;
import org.jdbi.core.internal.exceptions.Unchecked;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.core.qualifier.Qualifiers;
import org.jdbi.core.statement.StatementContext;

/**
 * Binds public methods with no parameters on a specified object.
 * @deprecated this functionality will remain supported, but this class should not be API
 */
@Deprecated(since = "3.11.0")
public class ObjectMethodArguments extends ObjectPropertyNamedArgumentFinder {
    private static final ConfigCache<Class<?>, Map<String, Function<Object, TypedValue>>> NULLARY_METHOD_CACHE =
            ConfigCaches.declare(ObjectMethodArguments::load);
    /**
     * @param prefix an optional prefix (we insert a '.' as a separator)
     * @param object the object to bind functions on
     */
    public ObjectMethodArguments(String prefix, Object object) {
        super(prefix, object);
    }

    private static Map<String, Function<Object, TypedValue>> load(ConfigRegistry config, Class<?> type) {
        final HashMap<String, Function<Object, TypedValue>> methodMap = new HashMap<>();
        if (Modifier.isPublic(type.getModifiers())) {
            Arrays.stream(type.getMethods())
                .filter(m -> m.getParameterCount() == 0)
                .filter(JdbiAnnotations::isBound)
                .collect(Collectors.toMap(
                        Method::getName,
                        Function.identity(),
                        ObjectMethodArguments::bridgeMethodMerge))
                .forEach((name, method) -> {
                    QualifiedType<?> qualifiedType = QualifiedType.of(method.getGenericReturnType())
                            .withAnnotations(config.get(Qualifiers.class).findFor(method));
                    MethodHandle mh = Unchecked.function(MethodHandles.lookup()::unreflect).apply(method);
                    methodMap.put(name, Unchecked.function(
                            value -> new TypedValue(qualifiedType, mh.invoke(value))));
                });
        } else {
            Optional.ofNullable(type.getSuperclass()).ifPresent(superclass -> methodMap.putAll(load(config, superclass)));
            Arrays.stream(type.getInterfaces()).forEach(interfaceClass -> methodMap.putAll(load(config, interfaceClass)));
        }
        return methodMap;
    }

    @Override
    protected Optional<TypedValue> getValue(String name, StatementContext ctx) {
        return getter(name, ctx.getConfig()).map(m -> m.apply(obj));
    }

    public Optional<Function<Object, TypedValue>> getter(String name, ConfigRegistry config) {
        return Optional.ofNullable(NULLARY_METHOD_CACHE.get(obj.getClass(), config).get(name));
    }

    @Override
    protected NamedArgumentFinder getNestedArgumentFinder(TypedValue value) {
        return new ObjectMethodArguments(null, value.getValue());
    }

    @Override
    public String toString() {
        return "{lazy object functions arguments \"" + obj + "\"";
    }

    private static Method bridgeMethodMerge(Method a, Method b) {
        return a.isBridge() ? b : a;
    }
}
