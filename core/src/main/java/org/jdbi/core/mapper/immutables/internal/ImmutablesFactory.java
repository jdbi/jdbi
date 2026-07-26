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
package org.jdbi.core.mapper.immutables.internal;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;
import java.util.function.Supplier;

import org.jdbi.core.internal.JdbiOptionals;
import org.jdbi.core.internal.exceptions.Unchecked;
import org.jdbi.core.mapper.reflect.internal.BuilderPojoPropertiesFactory;
import org.jdbi.core.mapper.reflect.internal.ModifiablePojoPropertiesFactory;
import org.jdbi.core.mapper.reflect.internal.PojoPropertiesFactory;

/**
 * Builds the {@link PojoPropertiesFactory} registrations that back {@code registerImmutable} /
 * {@code registerModifiable} for <a href="https://immutables.github.io">Immutables</a> generated value types.
 * The reflection is a pure function of the spec class, so it lives here rather than on a config value.
 */
public final class ImmutablesFactory {

    private ImmutablesFactory() {}

    /**
     * Build the registration for an {@code Immutable*} value class, expecting the default generated class and builder names.
     */
    public static <S> Registration immutable(Class<S> spec) {
        final Class<? extends S> impl = classByPrefix("Immutable", spec);
        return immutable(spec, impl, JdbiOptionals.findFirstPresent(
                () -> nullaryMethodOf(spec, "builder"),
                () -> nullaryMethodOf(impl, "builder"))
                    .orElseThrow(() -> new IllegalArgumentException("Neither " + spec + " nor " + impl + " have a 'builder' method")));
    }

    /**
     * Build the registration for an {@code Immutable*} value class, using a supplied implementation and builder.
     */
    public static <S, I extends S> Registration immutable(Class<S> spec, Class<I> impl, Supplier<?> builder) {
        return new Registration(spec, impl, BuilderPojoPropertiesFactory.builder(spec, builder));
    }

    /**
     * Build the registration for a {@code Modifiable*} value class, expecting the default generated class and public nullary constructor.
     */
    public static <S> Registration modifiable(Class<S> spec) {
        final Class<? extends S> impl = classByPrefix("Modifiable", spec);
        return modifiable(spec, impl,
                nullaryMethodOf(impl, "create")
                    .orElseGet(() -> constructorOf(impl)));
    }

    /**
     * Build the registration for a {@code Modifiable*} value class, using a supplied implementation and constructor.
     */
    public static <S, M extends S> Registration modifiable(Class<S> spec, Class<M> impl, Supplier<?> constructor) {
        return new Registration(spec, impl, ModifiablePojoPropertiesFactory.modifiable(spec, impl, () -> impl.cast(constructor.get())));
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

    private static <S> Class<? extends S> classByPrefix(String prefix, Class<S> spec) {
        final String implName = spec.getPackage().getName() + '.' + prefix + spec.getSimpleName();
        try {
            return Class.forName(implName).asSubclass(spec);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Couldn't locate default implementation class " + implName, e);
        }
    }

    /**
     * A single spec/impl class pair mapped to the {@link PojoPropertiesFactory} that handles both. The factory
     * is registered for both classes so binding and mapping work whether the spec or the generated implementation
     * is named.
     */
    public record Registration(Class<?> spec, Class<?> impl, PojoPropertiesFactory factory) {}
}
