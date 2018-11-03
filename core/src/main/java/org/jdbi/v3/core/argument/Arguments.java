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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jdbi.v3.core.array.SqlArrayArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;

import static org.jdbi.v3.core.internal.JdbiStreams.toStream;

/**
 * A registry for ArgumentFactory instances.
 * When a statement with bound parameters is executed, Jdbi consults the
 * Arguments registry to obtain an Argument instance for each bound parameter
 * (see #findFor(...)).
 * The factories are consulted in reverse order of registration (i.e. last-registered wins).
 */
public class Arguments implements JdbiConfig<Arguments> {
    private final List<ArgumentFactory> argumentFactories = new CopyOnWriteArrayList<>();
    private ConfigRegistry registry;
    private Argument untypedNullArgument = new NullArgument(Types.OTHER);

    public Arguments() {
        // TODO move to BuiltInSupportPlugin

        // the null factory must be interrogated last to preserve types!
        register(new UntypedNullArgumentFactory());

        register(new PrimitivesArgumentFactory());
        register(new BoxedArgumentFactory());
        register(new EssentialsArgumentFactory());
        register(new SqlArgumentFactory());
        register(new InternetArgumentFactory());
        register(new SqlTimeArgumentFactory());
        register(new JavaTimeArgumentFactory());
        register(new SqlArrayArgumentFactory());
        register(new JavaTimeZoneIdArgumentFactory());
        register(new EnumArgumentFactory());
        register(new OptionalArgumentFactory());
    }

    @Override
    public void setRegistry(ConfigRegistry registry) {
        this.registry = registry;
    }

    private Arguments(Arguments that) {
        argumentFactories.addAll(that.argumentFactories);
        untypedNullArgument = that.untypedNullArgument;
    }

    /**
     * Registers the given argument factory.
     * If more than one of the registered factories supports a given parameter type, the last-registered factory wins.
     * @param factory the factory to add
     * @return this
     */
    public Arguments register(ArgumentFactory factory) {
        argumentFactories.add(0, factory);
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
        return argumentFactories.stream()
                .flatMap(factory -> toStream(factory.build(type, value, registry)))
                .findFirst();
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

    @Override
    public Arguments createCopy() {
        return new Arguments(this);
    }
}
