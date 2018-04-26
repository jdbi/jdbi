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

import static org.jdbi.v3.core.internal.JdbiStreams.toStream;

import java.lang.reflect.Type;
import java.sql.Types;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.v3.core.argument.qualified.BuiltInQualifiedArgumentFactory;
import org.jdbi.v3.core.argument.qualified.QualifiedArgumentFactory;
import org.jdbi.v3.core.argument.qualified.QualifiedType;
import org.jdbi.v3.core.array.SqlArrayArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;

/**
 * A registry for ArgumentFactory instances.
 * When a statement with bound parameters is executed, Jdbi consults the
 * Arguments registry to obtain an Argument instance for each bound parameter
 * (see #findFor(...)).
 * The factories are consulted in reverse order of registration (i.e. last-registered wins).
 */
public class Arguments implements JdbiConfig<Arguments> {
    private final List<ArgumentFactory> argumentFactories = new CopyOnWriteArrayList<>();
    private final List<QualifiedArgumentFactory> qualifiedArgumentFactories = new CopyOnWriteArrayList<>();
    private ConfigRegistry registry;
    private Argument untypedNullArgument = new NullArgument(Types.OTHER);

    public Arguments() {
        register(BuiltInArgumentFactory.INSTANCE);
        register(new SqlArrayArgumentFactory());
        register(new JavaTimeZoneIdArgumentFactory());

        register(BuiltInQualifiedArgumentFactory.INSTANCE);
    }

    @Override
    public void setRegistry(ConfigRegistry registry) {
        this.registry = registry;
    }

    private Arguments(Arguments that) {
        argumentFactories.addAll(that.argumentFactories);
        qualifiedArgumentFactories.addAll(that.qualifiedArgumentFactories);
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
     * Registers the given qualified argument factory.
     * If more than one of the registered factories supports a given parameter type, the last-registered factory wins.
     * @param factory the factory to add
     * @return this
     */
    public Arguments register(QualifiedArgumentFactory factory) {
        qualifiedArgumentFactories.add(0, factory);
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
     * Obtain an argument for given value in the given context.
     *
     * @param type  the qualified type of the argument.
     * @param value the argument value.
     * @return an Argument for the given value.
     */
    public Optional<Argument> findFor(QualifiedType type, Object value) {
        if (type.getQualifiers().isEmpty()) {
            return findFor(type.getType(), value);
        }

        return qualifiedArgumentFactories.stream()
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
