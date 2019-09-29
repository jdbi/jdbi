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
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiCache;
import org.jdbi.v3.core.config.JdbiCaches;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.internal.exceptions.Sneaky;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.core.mapper.reflect.internal.BeanPropertiesFactory.BeanPojoProperties.PropertiesHolder;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;

public class BeanPropertiesFactory {
    private static final JdbiCache<Type, PropertiesHolder<?>> PROPERTY_CACHE =
            JdbiCaches.declare(t -> new PropertiesHolder<>(GenericTypes.getErasedType(t)));

    private BeanPropertiesFactory() {}

    public static PojoProperties<?> propertiesFor(Type t, ConfigRegistry config) {
        return new BeanPojoProperties<>(t, config);
    }

    private static boolean shouldSeeProperty(PropertyDescriptor pd) {
        // 'class' isn't really a property
        final Method read = pd.getReadMethod();
        return read == null || read.getDeclaringClass() != Object.class;
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
            return (Map) PROPERTY_CACHE.get(getType(), config).properties;
        }

        @Override
        public PojoBuilder<T> create() {
            final PropertiesHolder<?> holder = PROPERTY_CACHE.get(getType(), config);
            final T instance = (T) holder.constructor.get();
            return new PojoBuilder<T>() {
                @Override
                public void set(String property, Object value) {
                    holder.properties.get(property)
                        .setter()
                        .accept(instance, value);
                }

                @Override
                public T build() {
                    return instance;
                }
            };
        }

        static class BeanPojoProperty<T> implements PojoProperty<T> {
            final PropertyDescriptor descriptor;
            final QualifiedType<?> qualifiedType;
            final ConcurrentMap<Class<?>, Optional<Annotation>> annoCache = new ConcurrentHashMap<>();
            final Optional<Function<Object, Object>> getter;
            final Optional<BiConsumer<Object, Object>> setter;

            BeanPojoProperty(PropertyDescriptor property) {
                this.descriptor = property;
                this.qualifiedType = determineQualifiedType();
                getter = Optional.ofNullable(descriptor.getReadMethod())
                        .map(Unchecked.function(MethodHandles.lookup()::unreflect))
                        .map(mh -> Unchecked.function(mh::invoke));
                setter = Optional.ofNullable(descriptor.getWriteMethod())
                        .map(Unchecked.function(MethodHandles.lookup()::unreflect))
                        .map(mh -> Unchecked.biConsumer(mh::invoke));
            }

            protected Function<Object, Object> getter() {
                return getter.orElseThrow(() ->
                        new UnableToCreateStatementException(String.format("No getter method found for "
                            + "bean property [%s] on [%s]",
                            getName(), qualifiedType)));
            }

            protected BiConsumer<Object, Object> setter() {
                return setter.orElseThrow(() ->
                    new UnableToCreateStatementException(String.format("No setter method found for "
                        + "bean property [%s] on [%s]",
                        getName(), qualifiedType)));
            }

            @Override
            public String getName() {
                return descriptor.getName();
            }

            @Override
            public QualifiedType<?> getQualifiedType() {
                return qualifiedType;
            }

            private QualifiedType<?> determineQualifiedType() {
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
                return annoCache.computeIfAbsent(anno, x ->
                    Stream.of(descriptor.getReadMethod(), descriptor.getWriteMethod())
                        .filter(Objects::nonNull)
                        .map(m -> m.getAnnotation(anno))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .map(Annotation.class::cast))
                    .map(anno::cast);
            }

            @Override
            public Object get(T pojo) {
                return getter().apply(pojo);
            }
        }

        static class PropertiesHolder<T> {
            final Supplier<T> constructor;
            final Map<String, BeanPojoProperty<?>> properties;
            PropertiesHolder(Class<?> clazz) {
                try {
                    properties = Arrays.stream(Introspector.getBeanInfo(clazz).getPropertyDescriptors())
                            .filter(BeanPropertiesFactory::shouldSeeProperty)
                            .map(BeanPojoProperty::new)
                            .collect(Collectors.toMap(PojoProperty::getName, Function.identity()));
                } catch (IntrospectionException e) {
                    throw new IllegalArgumentException("Failed to inspect bean " + clazz, e);
                }
                Supplier<T> myConstructor;
                try {
                    MethodHandle ctorMh = MethodHandles.lookup()
                            .findConstructor(clazz, MethodType.methodType(void.class))
                            .asType(MethodType.methodType(clazz));
                    myConstructor = Unchecked.supplier(() -> (T) ctorMh.invoke());
                } catch (ReflectiveOperationException e) {
                    myConstructor = () -> {
                        throw Sneaky.throwAnyway(e);
                    };
                }
                constructor = myConstructor;
            }
        }
    }
}
