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

import static org.jdbi.v3.core.internal.JdbiOptionals.findFirstPresent;
import static org.jdbi.v3.core.internal.JdbiStreams.toStream;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.v3.core.ConfigRegistry;
import org.jdbi.v3.core.JdbiConfig;
import org.jdbi.v3.core.array.SqlArrayArgumentFactory;

public class Arguments implements JdbiConfig<Arguments> {

    private final Optional<Arguments> parent;
    private final List<ArgumentFactory> argumentFactories = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Type, Argument<?>> cache = new ConcurrentHashMap<>();

    public Arguments() {
        parent = Optional.empty();
        register(BuiltInArgumentFactory.INSTANCE);
        register(new SqlArrayArgumentFactory());
    }

    private Arguments(Arguments that) {
        parent = Optional.of(that);
    }

    public Arguments register(ArgumentFactory factory) {
        argumentFactories.add(0, factory);
        return this;
    }

    /**
     * Obtain an argument for given value in the given context
     *
     * @param type  the type of the argument.
     * @param config the config registry, for composition
     * @return an Argument for the given value.
     */
    public Optional<Argument> findFor(Type type, ConfigRegistry config) {
        return findFirstPresent(
                () -> argumentFactories.stream()
                        .flatMap(factory -> toStream(factory.build(type, config)))
                        .findFirst(),
                () -> parent.flatMap(p -> p.findFor(type, config)));
    }

    @Override
    public Arguments createChild() {
        return new Arguments(this);
    }
}
