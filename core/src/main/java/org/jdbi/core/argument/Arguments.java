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
package org.jdbi.core.argument;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import org.jdbi.core.array.SqlArrayArgumentFactory;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.internal.RegistrationLists;

/**
 * A registry for ArgumentFactory instances. Holds only registration data; resolving a factory into an
 * {@link Argument} for a bound value (and caching the result) is done per configuration registry by
 * {@link ArgumentResolver}.
 * The factories are consulted in reverse order of registration (i.e. last-registered wins).
 * <p>
 * This configuration is immutable: {@link #register} and the policy withers return a new instance, leaving
 * the receiver unchanged.
 */
public final class Arguments implements JdbiConfig<Arguments> {

    // The built-in factories are stateless, so a single immutable list is shared across all root registries.
    private static final List<QualifiedArgumentFactory> DEFAULT_FACTORIES = buildDefaultFactories();

    private final List<QualifiedArgumentFactory> factories;
    private final Argument untypedNullArgument;
    private final boolean bindingNullToPrimitivesPermitted;
    private final boolean preparedArgumentsEnabled;

    public Arguments() {
        this(DEFAULT_FACTORIES, new NullArgument(Types.OTHER), true, true);
    }

    private Arguments(final List<QualifiedArgumentFactory> factories,
            final Argument untypedNullArgument,
            final boolean bindingNullToPrimitivesPermitted,
            final boolean preparedArgumentsEnabled) {
        this.factories = factories;
        this.untypedNullArgument = untypedNullArgument;
        this.bindingNullToPrimitivesPermitted = bindingNullToPrimitivesPermitted;
        this.preparedArgumentsEnabled = preparedArgumentsEnabled;
    }

    private static List<QualifiedArgumentFactory> buildDefaultFactories() {
        // Register built-in factories; priority is by reverse registration order, so registration prepends.
        // The null factory must be interrogated last to preserve types, so it is registered first.
        final List<QualifiedArgumentFactory> factories = new ArrayList<>();
        prepend(factories, new UntypedNullArgumentFactory());

        prepend(factories, new PrimitivesArgumentFactory());
        prepend(factories, new BoxedArgumentFactory());
        prepend(factories, new SqlArgumentFactory());
        prepend(factories, new InternetArgumentFactory());
        prepend(factories, new SqlTimeArgumentFactory());
        prepend(factories, new JavaTimeArgumentFactory());
        prepend(factories, new SqlArrayArgumentFactory());
        prepend(factories, new CharSequenceArgumentFactory()); // register before EssentialsArgumentFactory which handles String
        prepend(factories, new EssentialsArgumentFactory());
        prepend(factories, new JavaTimeZoneIdArgumentFactory());
        prepend(factories, new NVarcharArgumentFactory());
        factories.add(0, new EnumArgumentFactory()); // natively a QualifiedArgumentFactory; no adaptation needed
        prepend(factories, new OptionalArgumentFactory());
        prepend(factories, new DirectArgumentFactory());
        return List.copyOf(factories);
    }

    private static void prepend(final List<QualifiedArgumentFactory> factories, final ArgumentFactory factory) {
        factories.add(0, QualifiedArgumentFactory.adapt(factory));
    }

    /**
     * Registers the given argument factory.
     * If more than one of the registered factories supports a given parameter type, the last-registered factory wins.
     * @param factory the factory to add
     * @return a copy of this configuration with the factory registered
     */
    @CheckReturnValue
    public Arguments register(final ArgumentFactory factory) {
        return register(QualifiedArgumentFactory.adapt(factory));
    }

    /**
     * Registers the given qualified argument factory.
     * If more than one of the registered factories supports a given parameter type, the last-registered factory wins.
     * @param factory the qualified factory to add
     * @return a copy of this configuration with the factory registered
     */
    @CheckReturnValue
    public Arguments register(final QualifiedArgumentFactory factory) {
        return new Arguments(RegistrationLists.prepend(factories, factory),
                untypedNullArgument, bindingNullToPrimitivesPermitted, preparedArgumentsEnabled);
    }

    /**
     * Registers all of the given argument factories in a single derivation, as if each were passed to
     * {@link #register(ArgumentFactory)} in iteration order. As with successive {@code register} calls, the
     * last factory in the collection has the highest priority. This is both more efficient and more readable
     * than chaining individual {@code register} calls when adding several factories at once.
     * @param factories the factories to add
     * @return a copy of this configuration with the factories registered
     */
    @CheckReturnValue
    public Arguments register(final Collection<? extends ArgumentFactory> factories) {
        if (factories.isEmpty()) {
            return this;
        }
        return new Arguments(RegistrationLists.prependAll(this.factories, factories, QualifiedArgumentFactory::adapt),
                untypedNullArgument, bindingNullToPrimitivesPermitted, preparedArgumentsEnabled);
    }

    /**
     * Returns the registered factories, most-recently-registered first. Consumed by {@link ArgumentResolver}.
     *
     * @return the registered argument factories
     */
    List<QualifiedArgumentFactory> getFactories() {
        return factories;
    }

    /**
     * Configure the {@link Argument} to use when binding a null
     * we don't have a type for.
     * @param untypedNullArgument the argument to bind
     * @return a copy of this configuration with the untyped null argument set
     */
    @CheckReturnValue
    public Arguments untypedNullArgument(final Argument untypedNullArgument) {
        if (untypedNullArgument == null) {
            throw new IllegalArgumentException("the Argument itself may not be null");
        }
        return new Arguments(factories, untypedNullArgument, bindingNullToPrimitivesPermitted, preparedArgumentsEnabled);
    }

    /**
     * Returns the untyped null argument.
     *
     * @return the untyped null argument.
     */
    public Argument getUntypedNullArgument() {
        return untypedNullArgument;
    }

    /**
     * Returns true if binding {@code null} to a variable declared as a primitive type is allowed.
     *
     * @return true if binding {@code null} to a variable declared as a primitive type is allowed.
     */
    public boolean isBindingNullToPrimitivesPermitted() {
        return bindingNullToPrimitivesPermitted;
    }

    /**
     * Whether binding {@code null} to a variable declared as a primitive type should be allowed.
     *
     * @param bindingNullToPrimitivesPermitted if true, {@code null} can be bound to a variable declared as a primitive type.
     * @return a copy of this configuration with the policy set
     */
    @CheckReturnValue
    public Arguments bindingNullToPrimitivesPermitted(final boolean bindingNullToPrimitivesPermitted) {
        return new Arguments(factories, untypedNullArgument, bindingNullToPrimitivesPermitted, preparedArgumentsEnabled);
    }

    /**
     * Returns true if prepared arguments binding is enabled. Settings this improves performance.
     *
     * @return true if prepared arguments binding is enabled.
     */
    public boolean isPreparedArgumentsEnabled() {
        return preparedArgumentsEnabled;
    }

    /**
     * Configure whether {@link ArgumentFactory.Preparable} factories will be processed before regular {@link ArgumentFactory}
     * instances are. This improves speed at a small cost to backwards compatibility. Please disable it if you require the old semantics.
     *
     * @param preparedArgumentsEnabled whether to enable preparable argument factories
     * @return a copy of this configuration with the policy set
     */
    @CheckReturnValue
    public Arguments preparedArgumentsEnabled(final boolean preparedArgumentsEnabled) {
        return new Arguments(factories, untypedNullArgument, bindingNullToPrimitivesPermitted, preparedArgumentsEnabled);
    }

}
