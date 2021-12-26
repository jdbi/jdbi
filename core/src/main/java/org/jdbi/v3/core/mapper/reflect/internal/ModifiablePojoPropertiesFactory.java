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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Supplier;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiCache;
import org.jdbi.v3.core.config.JdbiCaches;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;

public interface ModifiablePojoPropertiesFactory extends PojoPropertiesFactory {
    JdbiCache<ModifiableSpec<?, ?>, ModifiablePojoProperties<?, ?>> MODIFIABLE_CACHE =
            JdbiCaches.declare(s -> s.type, ModifiablePojoProperties::new);

    static <T, M extends T> PojoPropertiesFactory modifiable(Class<T> defn, Class<M> impl, Supplier<M> constructor) {
        return (t, config) -> MODIFIABLE_CACHE.get(new ModifiableSpec<>(t, config, defn, impl, constructor), config);
    }

    class ModifiablePojoProperties<T, M> extends BuilderPojoProperties<T, M> {
        ModifiablePojoProperties(ModifiableSpec<T, M> spec) {
            super(spec.type, spec.config, spec.defn, spec.impl, spec.constructor);
        }

        @Override
        protected BuilderPojoProperty<T> createProperty(String name, Method m) {
            final Type propertyType = GenericTypes.resolveType(m.getGenericReturnType(), getType());
            try {
                return new BuilderPojoProperty<>(
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
                return PojoBuilderUtils.alwaysSet();
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
                    Unchecked.biConsumer(getProperties().get(property).setter::invoke).accept(instance, value);
                }

                @SuppressWarnings("unchecked")
                @Override
                public T build() {
                    return (T) instance;
                }
            };
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
