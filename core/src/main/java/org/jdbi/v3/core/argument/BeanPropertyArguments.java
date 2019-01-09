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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import org.jdbi.v3.core.argument.internal.MethodReturnValueNamedArgumentFinder;
import org.jdbi.v3.core.argument.internal.TypedValue;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;

import static java.util.stream.Collectors.toMap;
import static org.jdbi.v3.core.qualifier.Qualifiers.getQualifiers;

/**
 * Inspect a {@link java.beans} style object and bind parameters
 * based on each of its discovered properties.
 */
public class BeanPropertyArguments extends MethodReturnValueNamedArgumentFinder {
    private static final Map<Class<?>, Map<String, PropertyDescriptor>> CLASS_PROPERTY_DESCRIPTORS = ExpiringMap
        .builder()
        .expiration(10, TimeUnit.MINUTES)
        .expirationPolicy(ExpirationPolicy.ACCESSED)
        .entryLoader((Class<?> type) -> {
            try {
                BeanInfo info = Introspector.getBeanInfo(type);
                return Stream.of(info.getPropertyDescriptors())
                    .filter(BeanPropertyArguments::shouldSeeProperty)
                    .collect(toMap(PropertyDescriptor::getName, Function.identity()));
            } catch (IntrospectionException e) {
                throw new UnableToCreateStatementException(
                    "Failed to introspect object which is supposed to be used to "
                        + "set named args for a statement via JavaBean properties", e);
            }
        })
        .build();

    private static boolean shouldSeeProperty(PropertyDescriptor pd) {
        // 'class' isn't really a property
        final Method read = pd.getReadMethod();
        return read == null || read.getDeclaringClass() != Object.class;
    }

    private final Map<String, PropertyDescriptor> propertyDescriptors;

    /**
     * @param prefix an optional prefix (we insert a '.' as a separator)
     * @param bean the bean to inspect and bind
     */
    public BeanPropertyArguments(String prefix, Object bean) {
        super(prefix, bean);

        this.propertyDescriptors = CLASS_PROPERTY_DESCRIPTORS.get(bean.getClass());
    }

    @Override
    protected Optional<TypedValue> getValue(String name, StatementContext ctx) {
        PropertyDescriptor descriptor = propertyDescriptors.get(name);

        if (descriptor == null) {
            return Optional.empty();
        }

        Method getter = getGetter(name, descriptor, ctx);
        Method setter = descriptor.getWriteMethod();
        Parameter setterParam = Optional.ofNullable(setter)
            .map(m -> m.getParameterCount() > 0 ? m.getParameters()[0] : null)
            .orElse(null);

        Type type = getter.getGenericReturnType();
        Set<Annotation> qualifiers = getQualifiers(getter, setter, setterParam);
        Object value = invokeMethod(getter, ctx);

        return Optional.of(new TypedValue(type, qualifiers, value));
    }

    private Method getGetter(String name, PropertyDescriptor descriptor, StatementContext ctx) {
        Method getter = descriptor.getReadMethod();

        if (getter == null) {
            throw new UnableToCreateStatementException(String.format("No getter method found for "
                    + "bean property [%s] on [%s]",
                name, obj), ctx);
        }

        return getter;
    }

    @Override
    public Collection<String> getNames() {
        return propertyDescriptors.keySet();
    }

    @Override
    protected NamedArgumentFinder getNestedArgumentFinder(Object o) {
        return new BeanPropertyArguments(null, o);
    }

    @Override
    public String toString() {
        return "{lazy bean property arguments \"" + obj + "\"";
    }
}
