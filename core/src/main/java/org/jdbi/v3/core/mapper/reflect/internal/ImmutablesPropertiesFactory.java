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
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiCache;
import org.jdbi.v3.core.config.JdbiCaches;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties.PojoProperty;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;

public interface ImmutablesPropertiesFactory {
    @SuppressWarnings("unchecked")
    JdbiCache<ImmutableSpec<?, ?>, ImmutablePojoProperties<?, ?>> IMMUTABLE_CACHE =
            JdbiCaches.declare(s -> s.type, ImmutablePojoProperties::new);
    @SuppressWarnings("unchecked")
    JdbiCache<ModifiableSpec<?, ?>, ModifiablePojoProperties<?, ?>> MODIFIABLE_CACHE =
            JdbiCaches.declare(s -> s.type, ModifiablePojoProperties::new);

    PojoProperties<?> create(Type type, ConfigRegistry config);

    static <T, B> ImmutablesPropertiesFactory immutable(Class<T> defn, Supplier<B> builder) {
        return (t, config) -> IMMUTABLE_CACHE.get(new ImmutableSpec<>(t, config, defn, builder), config);
    }

    static <T, M extends T> ImmutablesPropertiesFactory modifiable(Class<T> defn, Class<M> impl, Supplier<M> constructor) {
        return (t, config) -> MODIFIABLE_CACHE.get(new ModifiableSpec<>(t, config, defn, impl, constructor), config);
    }

    static MethodHandle alwaysSet() {
        return MethodHandles.dropArguments(MethodHandles.constant(boolean.class, true), 0, Object.class);
    }

    abstract class BasePojoProperties<T, B> extends PojoProperties<T> {
        private final Map<String, ImmutablesPojoProperty<T>> properties;
        protected final ConfigRegistry config;
        protected final Class<T> defn;
        protected final Class<?> impl;
        protected final Supplier<?> builder;

        BasePojoProperties(Type type, ConfigRegistry config, Class<T> defn, Class<?> impl, Supplier<B> builder) {
            super(type);
            this.config = config;
            this.defn = defn;
            this.impl = impl;
            this.builder = builder;
            properties = Arrays.stream(defn.getMethods())
                    .filter(BasePojoProperties::isProperty)
                    .map(p -> createProperty(propertyName(p), p))
                    .collect(Collectors.toMap(PojoProperty::getName, Function.identity()));
        }

        static String propertyName(Method m) {
            final String[] prefixes = new String[] {"get", "is"};
            final String name = m.getName();
            for (String prefix : prefixes) {
                if (name.startsWith(prefix)) {
                    return chopPrefix(name, prefix.length());
                }
            }
            return name;
        }

        private static String chopPrefix(final String name, int off) {
            return name.substring(off, off + 1).toLowerCase() + name.substring(off + 1);
        }

        private static boolean isProperty(Method m) {
            return m.getParameterCount() == 0
                && !m.isSynthetic()
                && !Modifier.isStatic(m.getModifiers())
                && m.getDeclaringClass() != Object.class;
        }

        @Override
        public Map<String, ImmutablesPojoProperty<T>> getProperties() {
            return properties;
        }

        abstract ImmutablesPojoProperty<T> createProperty(String name, Method m);
    }

    class ImmutablePojoProperties<T, B> extends BasePojoProperties<T, B> {
        private MethodHandle builderBuild;

        ImmutablePojoProperties(ImmutableSpec<T, B> spec) {
            super(spec.type, spec.config, spec.defn, null, spec.builder);
            try {
                builderBuild = MethodHandles.lookup().unreflect(builder.get().getClass().getMethod("build"));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalArgumentException("Failed to inspect Immutables " + defn, e);
            }
        }
        @Override
        protected ImmutablesPojoProperty<T> createProperty(String name, Method m) {
            final Class<?> builderClass = builder.get().getClass();
            try {
                final Type propertyType = GenericTypes.resolveType(m.getGenericReturnType(), getType());
                return new ImmutablesPojoProperty<T>(
                        name,
                        QualifiedType.of(propertyType).withAnnotations(config.get(Qualifiers.class).findFor(m)),
                        m,
                        alwaysSet(),
                        MethodHandles.lookup().unreflect(m).asFixedArity(),
                        findBuilderSetter(builderClass, name, propertyType).asFixedArity());
            } catch (IllegalAccessException | NoSuchMethodException e) {
                throw new IllegalArgumentException("Failed to inspect method " + m, e);
            }
        }

        private MethodHandle findBuilderSetter(final Class<?> builderClass, String name, Type type)
        throws IllegalAccessException, NoSuchMethodException {
            final List<NoSuchMethodException> failures = new ArrayList<>();
            final String setName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
            final Set<String> names = new HashSet<>(Arrays.asList(setName, name));
            for (String tryName : names) {
                try {
                    return MethodHandles.lookup().unreflect(builderClass.getMethod(tryName, GenericTypes.getErasedType(type)));
                } catch (NoSuchMethodException e) {
                    failures.add(e);
                }
            }
            for (Method m : builderClass.getMethods()) {
                if (names.contains(m.getName()) && m.getParameterCount() == 1) {
                    return MethodHandles.lookup().unreflect(m);
                }
            }
            final IllegalArgumentException iae = new IllegalArgumentException("Failed to find builder setter for property " + name + " on " + builderClass);
            failures.forEach(iae::addSuppressed);
            return MethodHandles.dropArguments(
                    MethodHandles.throwException(Object.class, IllegalArgumentException.class),
                    1, Arrays.asList(Object.class, Object.class))
                .bindTo(iae);
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

    class ModifiablePojoProperties<T, M> extends BasePojoProperties<T, M> {
        ModifiablePojoProperties(ModifiableSpec<T, M> spec) {
            super(spec.type, spec.config, spec.defn, spec.impl, spec.constructor);
        }

        @Override
        protected ImmutablesPojoProperty<T> createProperty(String name, Method m) {
            final Type propertyType = GenericTypes.resolveType(m.getGenericReturnType(), getType());
            try {
                return new ImmutablesPojoProperty<T>(
                        name,
                        QualifiedType.of(propertyType).withAnnotations(config.get(Qualifiers.class).findFor(m)),
                        m,
                        isSetMethod(name),
                        MethodHandles.lookup().unreflect(m).asFixedArity(),
                        MethodHandles.lookup().findVirtual(impl, setterName(name), MethodType.methodType(impl, GenericTypes.getErasedType(propertyType))).asFixedArity());
            } catch (IllegalAccessException | NoSuchMethodException e) {
                throw new IllegalArgumentException("Failed to inspect method " + m, e);
            }
        }

        private MethodHandle isSetMethod(String name) {
            try {
                return MethodHandles.lookup().findVirtual(impl, name + "IsSet", MethodType.methodType(boolean.class));
            } catch (NoSuchMethodException e) {
                // not optional field
                return alwaysSet();
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Failed to find IsSet method for " + name, e);
            }
        }

        private String setterName(String name) {
            return "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
        }

        @Override
        public PojoBuilder<T> create() {
            final Object instance = builder.get();
            return new PojoBuilder<T>() {
                @Override
                public void set(String property, Object value) {
                    Unchecked.biFunction(getProperties().get(property).setter::invoke).apply(instance, value);
                }

                @SuppressWarnings("unchecked")
                @Override
                public T build() {
                    return (T) instance;
                }
            };
        }
    }

    class ImmutablesPojoProperty<T> implements PojoProperty<T> {
        private final String name;
        private final QualifiedType<?> type;
        private final Method defn;
        private final MethodHandle isSet;
        private final MethodHandle getter;
        final MethodHandle setter;

        ImmutablesPojoProperty(String name, QualifiedType<?> type, Method defn, MethodHandle isSet, MethodHandle getter, MethodHandle setter) {
            this.name = name;
            this.type = type;
            this.defn = defn;
            this.isSet = isSet;
            this.getter = getter;
            this.setter = setter;
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
                if (Boolean.TRUE.equals(isSet.invoke(pojo))) {
                    return getter.invoke(pojo);
                } else {
                    return null;
                }
            }).call();
        }
    }

    class ImmutableSpec<T, B> {
        Type type;
        ConfigRegistry config;
        Class<T> defn;
        Supplier<B> builder;

        ImmutableSpec(Type type, ConfigRegistry config, Class<T> defn, Supplier<B> builder) {
            this.type = type;
            this.config = config;
            this.defn = defn;
            this.builder = builder;
        }
    }

    class ModifiableSpec<T, M> {
        Type type;
        ConfigRegistry config;
        Class<T> defn;
        Class<M> impl;
        Supplier<M> constructor;

        ModifiableSpec(Type type, ConfigRegistry config, Class<T> defn, Class<M> impl, Supplier<M> constructor) {
            this.type = type;
            this.config = config;
            this.defn = defn;
            this.impl = impl;
            this.constructor = constructor;
        }
    }
}
