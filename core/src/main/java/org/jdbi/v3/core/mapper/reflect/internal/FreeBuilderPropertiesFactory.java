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

import io.leangen.geantyref.GenericTypeReflector;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiCache;
import org.jdbi.v3.core.config.JdbiCaches;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties.PojoProperty;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public interface FreeBuilderPropertiesFactory extends BuilderPropertiesFactory {
    @SuppressWarnings("unchecked")
    JdbiCache<FreeBuilderSpec<?, ?>, FreeBuilderPojoProperties<?, ?>> FREE_BUILDER_CACHE =
        JdbiCaches.declare(s -> s.type, FreeBuilderPojoProperties::new);

    static <S> BuilderPropertiesFactory create(Class<S> spec, Class<? extends S> valueClass, Supplier<?> builder) {
        return (t, config) -> FREE_BUILDER_CACHE.get(new FreeBuilderSpec<>(t, config, spec, builder), config);
    }

    @Override
    default PojoProperties<?> create(Type type, ConfigRegistry config) {
        return null;
    }

    static MethodHandle alwaysSet() {
        return MethodHandles.dropArguments(MethodHandles.constant(boolean.class, true), 0, Object.class);
    }

    class FreeBuilderSpec<T,B> {
        Type type;
        ConfigRegistry config;
        Class<T> defn;
        Supplier<B> builder;

        FreeBuilderSpec(Type type, ConfigRegistry config, Class<T> defn, Supplier<B> builder) {
            this.type = type;
            this.config = config;
            this.defn = defn;
            this.builder = builder;
        }
    }

    class FreeBuilderPojoProperties<T,B> extends PojoProperties<T> {
        private final Map<String, FreeBuilderPojoProperty<T>> properties;
        protected final ConfigRegistry config;
        protected final Class<T> defn;
        protected final Class<?> value;
        protected final Supplier<?> builder;
        private MethodHandle builderBuild;

        FreeBuilderPojoProperties(Type type, ConfigRegistry config, Class<T> defn, Class<?> value, Supplier<B> builder) {
            super(type);
            this.config = config;
            this.defn = defn;
            this.value = value;
            this.builder = builder;
            properties = Arrays.stream(defn.getMethods())
                .filter(PojoBuilderUtils::isProperty)
                .map(p -> createProperty(PojoBuilderUtils.propertyName(p), p))
                .collect(Collectors.toMap(PojoProperty::getName, Function.identity()));
        }

        FreeBuilderPojoProperties(FreeBuilderSpec<T, B> spec) {
            this(spec.type, spec.config, spec.defn, null, spec.builder);

            try {
                builderBuild = MethodHandles.lookup().unreflect(builder.get().getClass().getMethod("build"));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalArgumentException("Failed to inspect Builder " + defn, e);
            }
        }

        @SuppressWarnings("unchecked")
        protected FreeBuilderPojoProperty<T> createProperty(String name, Method m) {
            final Class<?> builderClass = builder.get().getClass();
            try {
                final Type propertyType = GenericTypeReflector.getExactReturnType(m, getType());
                return new FreeBuilderPojoProperty<T>(
                    name,
                    QualifiedType.of(propertyType).withAnnotations(config.get(Qualifiers.class).findFor(m)),
                    m,
                    alwaysSet(),
                    MethodHandles.lookup().unreflect(m).asFixedArity(),
                    PojoBuilderUtils.findBuilderSetter(builderClass, name, m, propertyType).asFixedArity());
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Failed to inspect method " + m, e);
            }
        }

        @Override
        public Map<String, FreeBuilderPojoProperty<T>> getProperties() {
            return properties;
        }

        @Override
        public PojoBuilder<T> create() {
            final Object b = builder.get();
            return new PojoBuilder<T>() {
                @Override
                public void set(String property, Object value) {
                    Unchecked.biFunction(getProperties().get(property).setter::invoke).apply(b, value);
                }

                @Override
                public T build() {
                    return defn.cast(Unchecked.function(builderBuild::invoke).apply(b));
                }
            };
        }
    }

    class FreeBuilderPojoProperty<T> implements PojoProperty<T> {
        private final String name;
        private final QualifiedType<?> type;
        private final Method defn;
        private final MethodHandle isSet;
        private final MethodHandle getter;
        final MethodHandle setter;

        FreeBuilderPojoProperty(String name, QualifiedType<?> type, Method defn, MethodHandle isSet, MethodHandle getter, MethodHandle setter) {
            this.name = name;
            this.type = type;
            this.defn = defn;
            this.isSet = isSet;
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public String getName() { return name; }

        @Override
        public QualifiedType<?> getQualifiedType() { return type; }

        @Override
        public <A extends Annotation> Optional<A> getAnnotation(Class<A> anno) {
            return Optional.ofNullable(defn.getAnnotation(anno));
        }

        @Override
        public Object get(T pojo) {
            return Unchecked.callable(() -> {
                if (Boolean.TRUE.equals(isSet.invoke(pojo))) {
                    return getter.invoke(pojo);
                } else {
                    return null;
                }
            }).call();
        }
    }
}
