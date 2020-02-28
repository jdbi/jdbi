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
package org.jdbi.v3.core.argument;

import java.lang.reflect.Type;
import java.sql.Types;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import org.jdbi.v3.core.array.SqlArrayArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.internal.JdbiOptionals;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.meta.Beta;

/**
 * A registry for ArgumentFactory instances.
 * When a statement with bound parameters is executed, Jdbi consults the
 * Arguments registry to obtain an Argument instance for each bound parameter
 * (see #findFor(...)).
 * The factories are consulted in reverse order of registration (i.e. last-registered wins).
 */
public class Arguments implements JdbiConfig<Arguments> {
    private final List<QualifiedArgumentFactory> factories = new CopyOnWriteArrayList<>();
    private final Map<QualifiedType<?>, Function<Object, Argument>> preparedFactories;

    private ConfigRegistry registry;
    private Argument untypedNullArgument = new NullArgument(Types.OTHER);
    private boolean bindingNullToPrimitivesPermitted = true;

    public Arguments(ConfigRegistry registry) {
        this.registry = registry;
        preparedFactories = new ConcurrentHashMap<>();
        // TODO move to BuiltInSupportPlugin

        // the null factory must be interrogated last to preserve types!
        register(new UntypedNullArgumentFactory());

        register(new PrimitivesArgumentFactory());
        register(new BoxedArgumentFactory());
        register(new SqlArgumentFactory());
        register(new InternetArgumentFactory());
        register(new SqlTimeArgumentFactory());
        register(new JavaTimeArgumentFactory());
        register(new SqlArrayArgumentFactory());
        register(new EssentialsArgumentFactory());
        register(new JavaTimeZoneIdArgumentFactory());
        register(new NVarcharArgumentFactory());
        register(new EnumArgumentFactory());
        register(new OptionalArgumentFactory());
        register(new DirectArgumentFactory());
    }

    @Override
    public void setRegistry(ConfigRegistry registry) {
        this.registry = registry;
    }

    private Arguments(Arguments that) {
        factories.addAll(that.factories);
        preparedFactories = new ConcurrentHashMap<>(that.preparedFactories);
        untypedNullArgument = that.untypedNullArgument;
        bindingNullToPrimitivesPermitted = that.bindingNullToPrimitivesPermitted;
    }

    /**
     * Registers the given argument factory.
     * If more than one of the registered factories supports a given parameter type, the last-registered factory wins.
     * @param factory the factory to add
     * @return this
     */
    public Arguments register(ArgumentFactory factory) {
        return register(QualifiedArgumentFactory.adapt(registry, factory));
    }

    /**
     * Registers the given qualified argument factory.
     * If more than one of the registered factories supports a given parameter type, the last-registered factory wins.
     * @param factory the qualified factory to add
     * @return this
     */
    @Beta
    public Arguments register(QualifiedArgumentFactory factory) {
        factories.add(0, factory);
        if (factory instanceof QualifiedArgumentFactory.Preparable) {
            QualifiedArgumentFactory.Preparable qaf = (QualifiedArgumentFactory.Preparable) factory;
            qaf.prePreparedTypes()
                .forEach(t ->
                    preparedFactories.put(t,
                        qaf.prepare(t, registry)
                            .orElseThrow(() -> new IllegalStateException("Preparable " + t + " failed on " + qaf))));
        }
        return this;
    }

    /**
     * Obtain an argument for given value in the given context
     *
     * @param type  the type of the argument.
     * @param value the argument value.
     * @return an Argument for the given value.
     */
    public Optional<Argument> findFor(Type type, Object value) {
        return findFor(QualifiedType.of(type), value);
    }

    /**
     * Obtain an argument for given value in the given context.
     *
     * @param type  the qualified type of the argument.
     * @param value the argument value.
     * @return an Argument for the given value.
     */
    @Beta
    public Optional<Argument> findFor(QualifiedType<?> type, Object value) {
        Function<Object, Argument> prepared = preparedFactories.get(type);
        if (prepared != null) {
            return Optional.of(prepared.apply(value));
        }
        return factories.stream()
            .flatMap(factory -> JdbiOptionals.stream(factory.build(type, value, registry)))
            .findFirst();
    }

    /**
     * Obtain a prepared argument function for given type in the given context.
     *
     * @param type  the type of the argument.
     * @return an Argument factory function for the given value.
     */
    public Optional<Function<Object, Argument>> prepareFor(Type type) {
        return prepareFor(QualifiedType.of(type));
    }

    /**
     * Obtain a prepared argument function for given type in the given context.
     *
     * @param type  the qualified type of the argument.
     * @return an Argument factory function for the given value.
     */
    @Beta
    public Optional<Function<Object, Argument>> prepareFor(QualifiedType<?> type) {
        Function<Object, Argument> prepared = preparedFactories.get(type);
        if (prepared != null) {
            return Optional.of(prepared);
        }
        return factories.stream()
            .filter(QualifiedArgumentFactory.Preparable.class::isInstance)
            .map(QualifiedArgumentFactory.Preparable.class::cast)
            .flatMap(factory -> JdbiOptionals.stream(factory.prepare(type, registry)))
            .findFirst();
    }

    @Beta
    public List<QualifiedArgumentFactory> getFactories() {
        return Collections.unmodifiableList(factories);
    }

    /**
     * Configure the {@link Argument} to use when binding a null
     * we don't have a type for.
     * @param untypedNullArgument the argument to bind
     */
    public void setUntypedNullArgument(Argument untypedNullArgument) {
        if (untypedNullArgument == null) {
            throw new IllegalArgumentException("the Argument itself may not be null");
        }
        this.untypedNullArgument = untypedNullArgument;
    }

    /**
     * @return the untyped null argument
     */
    public Argument getUntypedNullArgument() {
        return untypedNullArgument;
    }

    /**
     * @return if binding {@code null} to a variable declared as a primitive type is allowed
     */
    public boolean isBindingNullToPrimitivesPermitted() {
        return bindingNullToPrimitivesPermitted;
    }

    /**
     * @param bindingNullToPrimitivesPermitted if binding {@code null} to a variable declared as a primitive type should be allowed
     */
    public void setBindingNullToPrimitivesPermitted(boolean bindingNullToPrimitivesPermitted) {
        this.bindingNullToPrimitivesPermitted = bindingNullToPrimitivesPermitted;
    }

    @Override
    public Arguments createCopy() {
        return new Arguments(this);
    }
}
