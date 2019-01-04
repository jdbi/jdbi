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
package org.jdbi.v3.core.mapper.immutables;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.internal.JdbiOptionals;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.core.mapper.immutables.internal.ImmutablesMapperFactory;
import org.jdbi.v3.core.mapper.reflect.internal.ImmutablesPropertiesFactory;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties;
import org.jdbi.v3.core.mapper.reflect.internal.PojoPropertiesFactories;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.meta.Beta;

/**
 * Install support for an Immutables generated {@code Immutable} or {@code Modifiable} value type.
 * Note that unlike most plugins, this plugin is expected to be installed multiple times, once for each value type you wish to handle.
 */
@Beta
public class ImmutablesPlugin<S> implements JdbiPlugin {
    private final Class<S> spec;
    private final Class<? extends S> impl;
    private final Function<Type, ? extends PojoProperties<S>> properties;

    private ImmutablesPlugin(Class<S> spec, Class<? extends S> impl, Function<Type, ? extends PojoProperties<S>> properties) {
        this.spec = spec;
        this.impl = impl;
        this.properties = properties;
    }

    /**
     * Register bean arguments and row mapping for an {@code Immutable*} value class, expecting the default generated class and builder names.
     * @param spec the specification interface or abstract class
     * @param <S> the specification class
     * @return a plugin that configures type mapping for the given class
     */
    public static <S> ImmutablesPlugin<S> forImmutable(Class<S> spec) {
        final Class<? extends S> impl = classByPrefix("Immutable", spec);
        return forImmutable(spec, impl, JdbiOptionals.findFirstPresent(
                () -> nullaryMethodOf(spec, "builder"),
                () -> nullaryMethodOf(impl, "builder"))
                    .orElseThrow(() -> new IllegalArgumentException("Neither " + spec + " nor " + impl + " have a 'builder' method")));
    }

    /**
     * Register bean arguments and row mapping for an {@code Immutable*} value class, using a supplied implementation and builder.
     * @param spec the specification interface or abstract class
     * @param impl the generated implementation class
     * @param builder a supplier of new Builder instances
     * @param <S> the specification class
     * @param <I> the implementation class
     * @return a plugin that configures type mapping for the given class
     */
    public static <S, I extends S> ImmutablesPlugin<S> forImmutable(Class<S> spec, Class<I> impl, Supplier<?> builder) {
        return new ImmutablesPlugin<S>(spec, impl, ImmutablesPropertiesFactory.immutable(spec, builder));
    }

    /**
     * Register bean arguments and row mapping for an {@code Modifiable*} value class, expecting the default generated class and public nullary constructor.
     * @param spec the specification interface or abstract class
     * @param <S> the specification class
     * @return a plugin that configures type mapping for the given class
     */
    public static <S> ImmutablesPlugin<S> forModifiable(Class<S> spec) {
        final Class<? extends S> impl = classByPrefix("Modifiable", spec);
        return forModifiable(spec, impl,
                nullaryMethodOf(impl, "create")
                    .orElseGet(() -> constructorOf(impl)));
    }

    /**
     * Register bean arguments and row mapping for an {@code Modifiable*} value class, using a supplied implementation and constructor.
     * @param spec the specification interface or abstract class
     * @param impl the modifiable class
     * @param constructor a supplier of new Modifiable instances
     * @param <S> the specification class
     * @param <M> the modifiable class
     * @return a plugin that configures type mapping for the given class
     */
    public static <S, M extends S> ImmutablesPlugin<S> forModifiable(Class<S> spec, Class<M> impl, Supplier<?> constructor) {
        return new ImmutablesPlugin<S>(spec, impl, ImmutablesPropertiesFactory.modifiable(spec, () -> impl.cast(constructor.get())));
    }

    private static Optional<Supplier<?>> nullaryMethodOf(Class<?> impl, String methodName) {
        try {
            return Optional.of(Unchecked.supplier(MethodHandles.lookup()
                                .unreflect(impl.getMethod(methodName))::invoke));
        } catch (ReflectiveOperationException e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static <S> Supplier<S> constructorOf(Class<S> impl) {
        try {
            return (Supplier<S>) Unchecked.supplier(MethodHandles.lookup().findConstructor(impl, MethodType.methodType(void.class))::invoke);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Couldn't find public constructor of " + impl, e);
        }
    }

    private static <S, Sub extends S> Class<? extends S> classByPrefix(String prefix, Class<S> spec) {
        final String implName = spec.getPackage().getName() + '.' + prefix + spec.getSimpleName();
        try {
            return Class.forName(implName).asSubclass(spec);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Couldn't locate default implementation class " + implName, e);
        }
    }

    @Override
    public void customizeJdbi(Jdbi jdbi) {
        jdbi.getConfig(PojoPropertiesFactories.class).register(new Factory());
        jdbi.registerRowMapper(new ImmutablesMapperFactory<S>(spec, impl, properties));
    }

    class Factory implements Function<Type, Optional<PojoProperties<?>>> {
        @Override
        public Optional<PojoProperties<?>> apply(Type t) {
            if (t == spec || t == impl) {
                return Optional.of(properties.apply(t));
            }
            return Optional.empty();
        }
    }
}
