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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.core.array.SqlArrayArgumentFactory;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.config.JdbiConfig;

/**
 * A registry for ArgumentFactory instances. Holds only registration data; resolving a factory into an
 * {@link Argument} for a bound value (and caching the result) is done per configuration registry by
 * {@link ArgumentResolver}.
 * The factories are consulted in reverse order of registration (i.e. last-registered wins).
 */
public class Arguments implements JdbiConfig<Arguments> {
    private final List<QualifiedArgumentFactory> factories;

    private ConfigRegistry registry;
    private Argument untypedNullArgument = new NullArgument(Types.OTHER);
    private boolean bindingNullToPrimitivesPermitted = true;
    private boolean preparedArgumentsEnabled = true;

    public Arguments(final ConfigRegistry registry) {
        factories = new CopyOnWriteArrayList<>();
        this.registry = registry;

        // register built-in factories, priority of factories is by reverse registration order

        // the null factory must be interrogated last to preserve types!
        register(new UntypedNullArgumentFactory());

        register(new PrimitivesArgumentFactory());
        register(new BoxedArgumentFactory());
        register(new SqlArgumentFactory());
        register(new InternetArgumentFactory());
        register(new SqlTimeArgumentFactory());
        register(new JavaTimeArgumentFactory());
        register(new SqlArrayArgumentFactory());
        register(new CharSequenceArgumentFactory()); // register before EssentialsArgumentFactory which handles String
        register(new EssentialsArgumentFactory());
        register(new JavaTimeZoneIdArgumentFactory());
        register(new NVarcharArgumentFactory());
        register(new EnumArgumentFactory());
        register(new OptionalArgumentFactory());
        register(new DirectArgumentFactory());
    }

    private Arguments(final Arguments that) {
        factories = new CopyOnWriteArrayList<>(that.factories);
        untypedNullArgument = that.untypedNullArgument;
        bindingNullToPrimitivesPermitted = that.bindingNullToPrimitivesPermitted;
        preparedArgumentsEnabled = that.preparedArgumentsEnabled;
    }

    @Override
    public void setRegistry(final ConfigRegistry registry) {
        this.registry = registry;
    }

    /**
     * Registers the given argument factory.
     * If more than one of the registered factories supports a given parameter type, the last-registered factory wins.
     * @param factory the factory to add
     * @return this
     */
    public Arguments register(final ArgumentFactory factory) {
        return register(QualifiedArgumentFactory.adapt(registry, factory));
    }

    /**
     * Registers the given qualified argument factory.
     * If more than one of the registered factories supports a given parameter type, the last-registered factory wins.
     * @param factory the qualified factory to add
     * @return this
     */
    public Arguments register(final QualifiedArgumentFactory factory) {
        factories.add(0, factory);
        return this;
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
     */
    public void setUntypedNullArgument(final Argument untypedNullArgument) {
        if (untypedNullArgument == null) {
            throw new IllegalArgumentException("the Argument itself may not be null");
        }
        this.untypedNullArgument = untypedNullArgument;
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
     */
    public void setBindingNullToPrimitivesPermitted(final boolean bindingNullToPrimitivesPermitted) {
        this.bindingNullToPrimitivesPermitted = bindingNullToPrimitivesPermitted;
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
     */
    public void setPreparedArgumentsEnabled(final boolean preparedArgumentsEnabled) {
        this.preparedArgumentsEnabled = preparedArgumentsEnabled;
    }

    @Override
    public Arguments createCopy() {
        return new Arguments(this);
    }
}
