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
package org.jdbi.v3.core.mapper.freebuilder;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.function.Supplier;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.core.mapper.reflect.internal.BuilderPojoPropertiesFactory;
import org.jdbi.v3.core.mapper.reflect.internal.PojoPropertiesFactory;
import org.jdbi.v3.core.mapper.reflect.internal.PojoTypes;
import org.jdbi.v3.meta.Beta;

/**
 * Configures support for an <a href="https://freebuilder.inferred.org">FreeBuilder</a> generated {@code Builder)} value type.
 */
@Beta
public class JdbiFreeBuilders implements JdbiConfig<JdbiFreeBuilders> {
    private ConfigRegistry registry;

    public JdbiFreeBuilders() {}

    @Override
    public void setRegistry(ConfigRegistry registry) {
        this.registry = registry;
    }

    @Override
    public JdbiFreeBuilders createCopy() {
        return new JdbiFreeBuilders();
    }

    /**
     * Register bean arguments and row mapping for a {@code Freebuilder} value class, expecting the default generated class and builder names.
     * @param spec the specification interface of abstract class
     * @param <S> the specification class
     * @return a plugin that configures type mapping for the given class
     */
    public <S> JdbiFreeBuilders registerFreeBuilder(Class<S> spec) {
        final Class<? extends S> impl = valueClass(spec);
        return registerFreeBuilder(spec, impl, builderConstructor(spec));
    }

    /**
     * Convenience method for registering many freebuilder types.
     * @see #registerFreeBuilder(Class)
     */
    public JdbiFreeBuilders registerFreeBuilder(Class<?>... specs) {
        return registerFreeBuilder(Arrays.asList(specs));
    }

    /**
     * Convenience method for registering many freebuilder types.
     * @see #registerFreeBuilder(Class)
     */
    public JdbiFreeBuilders registerFreeBuilder(Iterable<Class<?>> specs) {
        specs.forEach(this::registerFreeBuilder);
        return this;
    }

    /**
     * Register bean arguments and row mapping for a {@code FreeBuilder} value class, using a supplied implementation and constructor.
     * @param spec the specification interface or abstract class
     * @param impl the generated value class
     * @param builderConstructor a supplier of new Builder instances
     * @param <S> the specification class
     * @param <I> the generated value class
     * @return a plugin that configures type mapping for the given class
     */
    public <S, I extends S> JdbiFreeBuilders registerFreeBuilder(Class<S> spec, Class<I> impl, Supplier<?> builderConstructor) {
        return register(spec, impl, BuilderPojoPropertiesFactory.builder(spec, builderConstructor));
    }

    private <S> JdbiFreeBuilders register(Class<S> spec, Class<? extends S> impl, PojoPropertiesFactory factory) {
        registry.get(PojoTypes.class).register(spec, factory).register(impl, factory);
        return this;
    }

    private <S> Class<? extends S> valueClass(Class<S> spec) {
        final String valueName;
        if (spec.getEnclosingClass() == null) {
            valueName = spec.getPackage().getName() + "." + spec.getSimpleName() + "_" + "Builder" + "$" + "Value";
        } else {
            String enclosingName = spec.getEnclosingClass().getSimpleName();
            valueName = spec.getPackage().getName() + "." + enclosingName + "_" + spec.getSimpleName() + "_" + "Builder" + "$" + "Value";
        }
        try {
            return Class.forName(valueName).asSubclass(spec);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Couldn't locate value class " + valueName, e);
        }
    }

    @SuppressWarnings("unchecked")
    private <S> Supplier<?> builderConstructor(Class<S> spec) {
        final Class builderClass = builderClass(spec);
        try {
            return (Supplier<S>) Unchecked.supplier(
                MethodHandles
                    .lookup()
                    .findConstructor(builderClass, MethodType.methodType(void.class))::invoke);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Couldn't find public constructor of " + spec, e);
        }
    }

    private <S> Class builderClass(Class<S> spec) {
        final String builderName;
        if (spec.getEnclosingClass() == null) {
            builderName = spec.getPackage().getName() + "." + spec.getSimpleName() + "." + "Builder";
        } else {
            String enclosingName = spec.getEnclosingClass().getSimpleName();
            builderName = spec.getPackage().getName() + "." + enclosingName + "$" + spec.getSimpleName() + "$" + "Builder";
        }

        try {
            return Class.forName(builderName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Couldn't locate builder class " + builderName, e);
        }
    }
}
