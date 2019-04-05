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
package org.jdbi.v3.core.mapper.reflect.internal;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiCache;
import org.jdbi.v3.core.config.JdbiCaches;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.reflect.internal.BeanPropertiesFactory.BeanPojoProperties.BeanPojoProperty;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties.PojoProperty;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;

public class BeanPropertiesFactory {
    private static final JdbiCache<Type, Map<String, BeanPojoProperty<?>>> PROPERTY_CACHE =
            JdbiCaches.declare(BeanPropertiesFactory::getProperties0);

    private static final String TYPE_NOT_INSTANTIABLE =
        "A bean, %s, was mapped which was not instantiable";

    private static final String MISSING_SETTER =
        "No appropriate method to write property %s";

    private static final String SETTER_NOT_ACCESSIBLE =
        "Unable to access setter for property, %s";

    private static final String INVOCATION_TARGET_EXCEPTION =
        "Invocation target exception trying to invoker setter for the %s property";

    private static final String REFLECTION_ILLEGAL_ARGUMENT_EXCEPTION =
        "Write method of %s for property %s is not compatible with the value passed";

    private BeanPropertiesFactory() {}

    public static PojoProperties<?> propertiesFor(Type t, ConfigRegistry config) {
        return new BeanPojoProperties<>(t, config);
    }

    private static boolean shouldSeeProperty(PropertyDescriptor pd) {
        // 'class' isn't really a property
        final Method read = pd.getReadMethod();
        return read == null || read.getDeclaringClass() != Object.class;
    }

    private static Map<String, BeanPojoProperty<?>> getProperties0(Type t) {
        try {
            return Arrays.stream(Introspector.getBeanInfo(GenericTypes.getErasedType(t)).getPropertyDescriptors())
                    .filter(BeanPropertiesFactory::shouldSeeProperty)
                    .map(BeanPojoProperty::new)
                    .collect(Collectors.toMap(PojoProperty::getName, Function.identity()));
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException("Failed to inspect bean " + t, e);
        }
    }

    static class BeanPojoProperties<T> extends PojoProperties<T> {
        private final ConfigRegistry config;

        BeanPojoProperties(Type type, ConfigRegistry config) {
            super(type);
            this.config = config;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public Map<String, BeanPojoProperty<T>> getProperties() {
            return (Map) PROPERTY_CACHE.get(getType(), config);
        }

        @SuppressWarnings("unchecked")
        @Override
        public PojoBuilder<T> create() {
            final Class<?> type = GenericTypes.getErasedType(getType());
            final T instance;
            try {
                instance = (T) type.newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format(TYPE_NOT_INSTANTIABLE, type.getName()), e);
            }
            return new PojoBuilder<T>() {
                @Override
                public void set(String property, Object value) {
                    final BeanPojoProperty<T> prop = getProperties().get(property);
                    try {
                        Method writeMethod = prop.descriptor.getWriteMethod();
                        if (writeMethod == null) {
                            throw new IllegalArgumentException(String.format(MISSING_SETTER, property));
                        }
                        writeMethod.invoke(instance, value);
                    } catch (IllegalAccessException e) {
                        throw new IllegalArgumentException(String.format(SETTER_NOT_ACCESSIBLE, property), e);
                    } catch (InvocationTargetException e) {
                        throw new IllegalArgumentException(String.format(INVOCATION_TARGET_EXCEPTION, property), e);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(String.format(REFLECTION_ILLEGAL_ARGUMENT_EXCEPTION,
                            prop.getQualifiedType(), prop.getName()), e);
                    }
                }

                @Override
                public T build() {
                    return instance;
                }
            };
        }

        static class BeanPojoProperty<T> implements PojoProperty<T> {
            final PropertyDescriptor descriptor;

            BeanPojoProperty(PropertyDescriptor property) {
                this.descriptor = property;
            }

            @Override
            public String getName() {
                return descriptor.getName();
            }

            @Override
            public QualifiedType<?> getQualifiedType() {
                Parameter setterParam = Optional.ofNullable(descriptor.getWriteMethod())
                    .map(m -> m.getParameterCount() > 0 ? m.getParameters()[0] : null)
                    .orElse(null);

                return QualifiedType.of(
                    Optional.ofNullable(descriptor.getReadMethod())
                        .map(Method::getGenericReturnType)
                        .orElseGet(() -> descriptor.getWriteMethod().getGenericParameterTypes()[0]))
                    .withAnnotations(
                        new Qualifiers().findFor(descriptor.getReadMethod(), descriptor.getWriteMethod(), setterParam));
            }

            @Override
            public <A extends Annotation> Optional<A> getAnnotation(Class<A> anno) {
                return Stream.of(descriptor.getReadMethod(), descriptor.getWriteMethod())
                        .filter(Objects::nonNull)
                        .map(m -> m.getAnnotation(anno))
                        .filter(Objects::nonNull)
                        .findFirst();
            }

            @Override
            public Object get(T pojo) {
                Method getter = descriptor.getReadMethod();

                if (getter == null) {
                    throw new UnableToCreateStatementException(String.format("No getter method found for "
                            + "bean property [%s] on [%s]",
                        getName(), pojo));
                }

                try {
                    return getter.invoke(pojo);
                } catch (IllegalAccessException e) {
                    throw new UnableToCreateStatementException(String.format("Access exception invoking "
                            + "method [%s] on [%s]",
                            getter.getName(), pojo), e);
                } catch (InvocationTargetException e) {
                    throw new UnableToCreateStatementException(String.format("Invocation target exception invoking "
                            + "method [%s] on [%s]",
                            getter.getName(), pojo), e);
                }
            }
        }
    }
}
