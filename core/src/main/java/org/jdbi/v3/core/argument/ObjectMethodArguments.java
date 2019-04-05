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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jdbi.v3.core.argument.internal.MethodReturnValueNamedArgumentFinder;
import org.jdbi.v3.core.argument.internal.TypedValue;
import org.jdbi.v3.core.config.JdbiCache;
import org.jdbi.v3.core.config.JdbiCaches;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * Binds public methods with no parameters on a specified object.
 */
public class ObjectMethodArguments extends MethodReturnValueNamedArgumentFinder {
    private static final JdbiCache<Class<?>, Map<String, Method>> NULLARY_METHOD_CACHE =
            JdbiCaches.declare(ObjectMethodArguments::load);
    /**
     * @param prefix an optional prefix (we insert a '.' as a separator)
     * @param object the object to bind functions on
     */
    public ObjectMethodArguments(String prefix, Object object) {
        super(prefix, object);
    }

    private static Map<String, Method> load(Class<?> type) {
        if (Modifier.isPublic(type.getModifiers())) {
            return Arrays.stream(type.getMethods())
                .filter(m -> m.getParameterCount() == 0)
                .collect(Collectors.toMap(Method::getName, Function.identity(), ObjectMethodArguments::bridgeMethodMerge));
        } else {
            final HashMap<String, Method> methodMap = new HashMap<>();
            Optional.ofNullable(type.getSuperclass()).ifPresent(superclass -> methodMap.putAll(load(superclass)));
            Arrays.stream(type.getInterfaces()).forEach(interfaceClass -> methodMap.putAll(load(interfaceClass)));
            return methodMap;
        }
    }

    @Override
    protected Optional<TypedValue> getValue(String name, StatementContext ctx) {
        Method method = NULLARY_METHOD_CACHE.get(obj.getClass(), ctx).get(name);

        if (method == null) {
            return Optional.empty();
        }

        QualifiedType<?> type = QualifiedType.of(method.getGenericReturnType())
                                .withAnnotations(ctx.getConfig(Qualifiers.class).findFor(method));
        Object value = invokeMethod(method, ctx);

        return Optional.of(new TypedValue(type, value));
    }

    @Override
    protected NamedArgumentFinder getNestedArgumentFinder(Object obj) {
        return new ObjectMethodArguments(null, obj);
    }

    @Override
    public String toString() {
        return "{lazy object functions arguments \"" + obj + "\"";
    }

    private static Method bridgeMethodMerge(Method a, Method b) {
        return a.isBridge() ? b : a;
    }
}
