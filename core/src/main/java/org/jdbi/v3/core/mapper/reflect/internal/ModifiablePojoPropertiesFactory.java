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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.function.Supplier;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.internal.ConfigCache;
import org.jdbi.v3.core.config.internal.ConfigCaches;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.internal.JdbiClassUtils;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ModifiablePojoPropertiesFactory extends PojoPropertiesFactory {
    ConfigCache<ModifiableSpec<?, ?>, ModifiablePojoProperties<?, ?>> MODIFIABLE_CACHE =
            ConfigCaches.declare(s -> s.type, ModifiablePojoProperties::new);

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
                        JdbiClassUtils.unreflect(config, m).asFixedArity(),
                        JdbiClassUtils.unreflect(config, setterMethod(name, GenericTypes.getErasedType(propertyType))).asFixedArity());
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("Failed to inspect method " + m, e);
            }
        }

        private Method setterMethod(String name, Class<?> propertyType) throws NoSuchMethodException {
            final Method setter = impl.getMethod(setterName(name), propertyType);
            if (Modifier.isStatic(setter.getModifiers()) || setter.getReturnType() != impl) {
                throw new NoSuchMethodException(impl.getName() + "." + setter.getName() + " is not an instance method that returns " + impl.getName());
            }
            return setter;
        }

        private MethodHandle isSetMethod(String name) {
            try {
                final Method isSet = impl.getMethod(name + "IsSet");
                if (!Modifier.isStatic(isSet.getModifiers()) && isSet.getReturnType() == boolean.class) {
                    return JdbiClassUtils.unreflect(config, isSet);
                }
                return PojoBuilderUtils.alwaysSet();
            } catch (NoSuchMethodException e) {
                // not optional field
                return PojoBuilderUtils.alwaysSet();
            }
        }

        /**
         * Takes a property name and turns it into a setter name.
         * @param name the property name.
         * @return The setter name.
         * @see java.beans.Introspector#decapitalize(String)
         */
        private String setterName(String name) {
            return "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }

        @Override
        public PojoBuilder<T> create() {
            final Object instance = builder.get();
            return new PojoBuilder<>() {
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
