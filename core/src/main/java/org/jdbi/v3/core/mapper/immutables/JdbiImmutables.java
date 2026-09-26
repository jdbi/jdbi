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

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.internal.JdbiClassUtils;
import org.jdbi.v3.core.internal.JdbiOptionals;
import org.jdbi.v3.core.internal.MemoizingSupplier;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.core.mapper.reflect.internal.BuilderPojoPropertiesFactory;
import org.jdbi.v3.core.mapper.reflect.internal.ModifiablePojoPropertiesFactory;
import org.jdbi.v3.core.mapper.reflect.internal.PojoPropertiesFactory;
import org.jdbi.v3.core.mapper.reflect.internal.PojoTypes;
import org.jdbi.v3.meta.Beta;

/**
 * Configures support for an <a href="https://immutables.github.io">Immutables</a> generated {@code Immutable} or {@code Modifiable} value type.
 */
@Beta
public class JdbiImmutables implements JdbiConfig<JdbiImmutables> {
    private ConfigRegistry registry;

    public JdbiImmutables() {}

    @Override
    public void setRegistry(ConfigRegistry registry) {
        this.registry = registry;
    }

    /**
     * Register bean arguments and row mapping for an {@code Immutable*} value class, expecting the default generated class and builder names.
     * @param spec the specification interface or abstract class
     * @param <S> the specification class
     * @return a plugin that configures type mapping for the given class
     */
    public <S> JdbiImmutables registerImmutable(Class<S> spec) {
        final Class<? extends S> impl = classByPrefix("Immutable", spec);
        return registerImmutable(spec, impl, JdbiOptionals.findFirstPresent(
                () -> nullaryMethodOf(spec, "builder"),
                () -> nullaryMethodOf(impl, "builder"))
                    .orElseThrow(() -> new IllegalArgumentException("Neither " + spec + " nor " + impl + " have a 'builder' method")));
    }

    /**
     * Convenience method for registering many immutable types.
     * @see #registerImmutable(Class)
     */
    public JdbiImmutables registerImmutable(Class<?>... specs) {
        return registerImmutable(Arrays.asList(specs));
    }

    /**
     * Convenience method for registering many immutable types.
     * @see #registerImmutable(Class)
     */
    public JdbiImmutables registerImmutable(Iterable<Class<?>> specs) {
        specs.forEach(this::registerImmutable);
        return this;
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
    public <S, I extends S> JdbiImmutables registerImmutable(Class<S> spec, Class<I> impl, Supplier<?> builder) {
        return register(spec, impl, BuilderPojoPropertiesFactory.builder(spec, builder));
    }

    /**
     * Convenience method for registering many modifiable types.
     * @see #registerModifiable(Class)
     */
    public JdbiImmutables registerModifiable(Class<?>... specs) {
        return registerModifiable(Arrays.asList(specs));
    }

    /**
     * Convenience method for registering many modifiable types.
     * @see #registerModifiable(Class)
     */
    public JdbiImmutables registerModifiable(Iterable<Class<?>> specs) {
        specs.forEach(this::registerModifiable);
        return this;
    }

    /**
     * Register bean arguments and row mapping for an {@code Modifiable*} value class, expecting the default generated class and public nullary constructor.
     * @param spec the specification interface or abstract class
     * @param <S> the specification class
     * @return a plugin that configures type mapping for the given class
     */
    public <S> JdbiImmutables registerModifiable(Class<S> spec) {
        final Class<? extends S> impl = classByPrefix("Modifiable", spec);
        return registerModifiable(spec, impl,
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
    public <S, M extends S> JdbiImmutables registerModifiable(Class<S> spec, Class<M> impl, Supplier<?> constructor) {
        return register(spec, impl, ModifiablePojoPropertiesFactory.modifiable(spec, impl, () -> impl.cast(constructor.get())));
    }

    private JdbiImmutables register(Class<?> spec, Class<?> impl, PojoPropertiesFactory factory) {
        registry.get(PojoTypes.class).register(spec, factory).register(impl, factory);
        return this;
    }

    private Optional<Supplier<?>> nullaryMethodOf(Class<?> impl, String methodName) {
        final Method method;
        try {
            method = impl.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
        final ConfigRegistry config = registry;
        return Optional.of(lazyInvoker(() -> JdbiClassUtils.unreflect(config, method)));
    }

    @SuppressWarnings("unchecked")
    private <S> Supplier<S> constructorOf(Class<S> impl) {
        final Constructor<S> constructor;
        try {
            constructor = impl.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Couldn't find public constructor of " + impl, e);
        }
        final ConfigRegistry config = registry;
        return (Supplier<S>) lazyInvoker(() -> JdbiClassUtils.unreflectConstructor(config, constructor));
    }

    // Resolve the handle on first use rather than at registration, so that an accessible object
    // strategy set on the Jdbi after registration still applies.
    private static Supplier<Object> lazyInvoker(Supplier<MethodHandle> handleFactory) {
        final Supplier<MethodHandle> handle = MemoizingSupplier.of(handleFactory);
        return Unchecked.supplier(() -> handle.get().invoke());
    }

    /**
     * The generated class lives next to the spec, so it must be loaded through the same class loader.
     * A plain {@code Class.forName(name)} would use the loader of this class, which fails in environments
     * that isolate application code from Jdbi (plugin containers, application servers).
     */
    private static <S> Class<? extends S> classByPrefix(String prefix, Class<S> spec) {
        final String implName = spec.getPackage().getName() + '.' + prefix + spec.getSimpleName();
        try {
            return Class.forName(implName, true, spec.getClassLoader()).asSubclass(spec);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Couldn't locate default implementation class " + implName, e);
        }
    }

    @Override
    public JdbiImmutables createCopy() {
        return new JdbiImmutables();
    }
}
