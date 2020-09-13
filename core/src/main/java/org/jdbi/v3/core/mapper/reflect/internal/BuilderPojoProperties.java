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

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.leangen.geantyref.GenericTypeReflector;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;

public class BuilderPojoProperties<T, B> extends PojoProperties<T> {
    protected MethodHandle builderBuild;
    private final Map<String, BuilderPojoProperty<T>> properties;
    protected final ConfigRegistry config;
    protected final Class<T> defn;
    protected final Class<?> impl;
    protected final Supplier<?> builder;

    BuilderPojoProperties(Type type, ConfigRegistry config, Class<T> defn, Class<?> impl, Supplier<B> builder) {
        super(type);
        this.config = config;
        this.defn = defn;
        this.impl = impl;
        this.builder = builder;
        properties = Arrays.stream(defn.getMethods())
            .filter(PojoBuilderUtils::isProperty)
            .map(p -> createProperty(PojoBuilderUtils.propertyName(p), p))
            .collect(Collectors.toMap(PojoProperty::getName, Function.identity()));
    }

    BuilderPojoProperties(BuilderSpec<T, B> spec) {
        this(spec.type, spec.config, spec.defn, null, spec.builder);
        try {
            builderBuild = MethodHandles.lookup().unreflect(builder.get().getClass().getMethod("build"))
                    .asType(MethodType.methodType(Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalArgumentException("Failed to inspect Immutables " + defn, e);
        }
    }

    @Override
    public Map<String, BuilderPojoProperty<T>> getProperties() {
        return properties;
    }

    protected BuilderPojoProperty<T> createProperty(String name, Method m) {
        final Class<?> builderClass = builder.get().getClass();
        try {
            final Type propertyType = GenericTypeReflector.getExactReturnType(m, getType());
            return new BuilderPojoProperty<>(
                name,
                QualifiedType.of(propertyType).withAnnotations(config.get(Qualifiers.class).findFor(m)),
                m,
                PojoBuilderUtils.alwaysSet(),
                MethodHandles.lookup().unreflect(m).asFixedArity(),
                PojoBuilderUtils.findBuilderSetter(builderClass, name, m, propertyType).asFixedArity());
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Failed to inspect method " + m, e);
        }
    }

    @Override
    public PojoBuilder<T> create() {
        final Object b = builder.get();
        return new PojoBuilder<T>() {
            @Override
            public void set(String property, Object value) {
                Unchecked.biConsumer(getProperties().get(property).setter::invokeExact).accept(b, value);
            }

            @Override
            public T build() {
                return defn.cast(Unchecked.function(builderBuild::invokeExact).apply(b));
            }
        };
    }

    public static class BuilderPojoProperty<T> implements PojoProperty<T> {
        private final String name;
        private final QualifiedType<?> type;
        private final Method defn;
        private final MethodHandle isSet;
        private final MethodHandle getter;
        final MethodHandle setter;

        BuilderPojoProperty(String name, QualifiedType<?> type, Method defn, MethodHandle isSet, MethodHandle getter, MethodHandle setter) {
            this.name = name;
            this.type = type;
            this.defn = defn;
            this.isSet = isSet.asType(MethodType.methodType(Boolean.class, Object.class));
            this.getter = getter.asType(MethodType.methodType(Object.class, Object.class));
            this.setter = setter.asType(MethodType.methodType(void.class, Object.class, Object.class));
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public QualifiedType<?> getQualifiedType() {
            return type;
        }

        @Override
        public <A extends Annotation> Optional<A> getAnnotation(Class<A> anno) {
            return Optional.ofNullable(defn.getAnnotation(anno));
        }

        @Override
        public Object get(T pojo) {
            return Unchecked.callable(() -> {
                if (Boolean.TRUE.equals((Boolean) isSet.invokeExact(pojo))) {
                    return getter.invokeExact(pojo);
                } else {
                    return null;
                }
            }).call();
        }
    }
}
