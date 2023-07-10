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

import io.leangen.geantyref.GenericTypeReflector;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.internal.ConfigCache;
import org.jdbi.v3.core.config.internal.ConfigCaches;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.internal.exceptions.Sneaky;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.core.mapper.reflect.internal.BeanPropertiesFactory.BeanPojoProperties.PropertiesHolder;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;

public class BeanPropertiesFactory {

    private static final ConfigCache<Type, PropertiesHolder<?>> PROPERTY_CACHE =
            ConfigCaches.declare(PropertiesHolder::new);

    private BeanPropertiesFactory() {}

    public static PojoProperties<?> propertiesFor(Type t, ConfigRegistry config) {
        return new BeanPojoProperties<>(t, config);
    }

    private static boolean shouldSeeProperty(PropertyDescriptor pd) {
        final Method read = pd.getReadMethod();
        if (read == null) {
            return pd.getWriteMethod() != null;
        }
        // 'class' isn't really a property
        return read.getParameterCount() == 0
                && read.getDeclaringClass() != Object.class;
    }

    static class BeanPojoProperties<T> extends PojoProperties<T> {

        private final ConfigRegistry config;

        BeanPojoProperties(Type type, ConfigRegistry config) {
            super(type);
            this.config = config;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public Map<String, BeanPojoProperty<T>> getProperties() {
            return (Map) PROPERTY_CACHE.get(getType(), config).properties;
        }

        @Override
        public PojoBuilder<T> create() {
            final PropertiesHolder<?> holder = PROPERTY_CACHE.get(getType(), config);
            @SuppressWarnings("unchecked")
            final T instance = (T) holder.getInstance();
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
            final Function<Object, Object> getter;
            final BiConsumer<Object, Object> setter;
            final Type actualBeanType;

            BeanPojoProperty(PropertyDescriptor property, Type actualBeanType) {
                this.descriptor = property;
                this.actualBeanType = actualBeanType;
                this.qualifiedType = determineQualifiedType();
                getter = Optional.ofNullable(descriptor.getReadMethod())
                        .map(Unchecked.function(MethodHandles.lookup()::unreflect))
                        .map(mh -> mh.asType(MethodType.methodType(Object.class, Object.class)))
                        .map(mh -> Unchecked.function(mh::invokeExact))
                        .orElse(null);
                setter = Optional.ofNullable(descriptor.getWriteMethod())
                        .map(Unchecked.function(MethodHandles.lookup()::unreflect))
                        .map(mh -> mh.asType(MethodType.methodType(void.class, Object.class, Object.class)))
                        .map(mh -> Unchecked.biConsumer(mh::invokeExact))
                        .orElse(null);
            }

            protected Function<Object, Object> getter() {
                if (getter == null) {
                    throw new UnableToCreateStatementException(String.format(
                            "No getter method found for bean property [%s] on [%s]",
                            getName(), qualifiedType));
                }
                return getter;
            }

            protected BiConsumer<Object, Object> setter() {
                if (setter == null) {
                    throw new UnableToCreateStatementException(String.format(
                            "No setter method found for bean property [%s] on [%s]",
                            getName(), qualifiedType));
                }
                return setter;
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
                                GenericTypeReflector.reduceBounded(GenericTypeReflector.annotate(
                                                GenericTypeReflector.resolveExactType(
                                                        Optional.ofNullable(descriptor.getReadMethod())
                                                                .map(m -> GenericTypeReflector.getExactReturnType(m, actualBeanType))
                                                                .orElseGet(() -> GenericTypeReflector.getExactParameterTypes(descriptor.getWriteMethod(), actualBeanType)[0]),
                                                        actualBeanType)))
                                        .getType())
                        .withAnnotations(
                                new Qualifiers().findFor(descriptor.getReadMethod(), descriptor.getWriteMethod(), setterParam));
            }

            @Override
            public <A extends Annotation> Optional<A> getAnnotation(Class<A> anno) {
                return annoCache.computeIfAbsent(anno, x ->
                                Stream.of(descriptor.getWriteMethod(), descriptor.getReadMethod())
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
            private final Supplier<T> constructor;
            private final Map<String, BeanPojoProperty<?>> properties;

            PropertiesHolder(Type type) {
                final Class<?> clazz = GenericTypes.getErasedType(type);
                try {
                    properties = Arrays.stream(Introspector.getBeanInfo(clazz).getPropertyDescriptors())
                            .filter(BeanPropertiesFactory::shouldSeeProperty)
                            .map(p -> new BeanPojoProperty<>(p, addMissingWildcards(type)))
                            .collect(Collectors.toMap(PojoProperty::getName, Function.identity()));
                } catch (IntrospectionException e) {
                    throw new IllegalArgumentException("Failed to inspect bean " + clazz, e);
                }
                Supplier<T> myConstructor;
                try {
                    MethodHandle ctorMh = MethodHandles.lookup()
                            .findConstructor(clazz, MethodType.methodType(void.class))
                            .asType(MethodType.methodType(Object.class));
                    myConstructor = Unchecked.supplier(() -> (T) ctorMh.invokeExact());
                } catch (ReflectiveOperationException e) {
                    myConstructor = () -> {
                        throw Sneaky.throwAnyway(e);
                    };
                }
                constructor = myConstructor;
            }

            T getInstance() {
                return constructor.get();
            }

            private Type addMissingWildcards(Type type) {
                if (GenericTypeReflector.isMissingTypeParameters(type)) {
                    return GenericTypeReflector.addWildcardParameters(
                            GenericTypeReflector.erase(type));
                }
                return type;
            }
        }
    }
}
