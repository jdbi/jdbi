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
package org.jdbi.v3.core.collector;

import java.lang.reflect.Type;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Collector;

import static org.jdbi.v3.core.collector.OptionalCollectors.toOptionalDouble;
import static org.jdbi.v3.core.collector.OptionalCollectors.toOptionalInt;
import static org.jdbi.v3.core.collector.OptionalCollectors.toOptionalLong;

class OptionalPrimitiveCollectorFactory implements CollectorFactory {
    private static final IdentityHashMap<Class<?>, Collector<?, ?, ?>> COLLECTORS = new IdentityHashMap<>();
    private static final IdentityHashMap<Class<?>, Class<?>> ELEMENT_TYPES = new IdentityHashMap<>();

    static {
        COLLECTORS.put(OptionalInt.class, toOptionalInt());
        ELEMENT_TYPES.put(OptionalInt.class, Integer.class);

        COLLECTORS.put(OptionalLong.class, toOptionalLong());
        ELEMENT_TYPES.put(OptionalLong.class, Long.class);

        COLLECTORS.put(OptionalDouble.class, toOptionalDouble());
        ELEMENT_TYPES.put(OptionalDouble.class, Double.class);
    }

    @Override
    public boolean accepts(Type containerType) {
        return containerType instanceof Class && COLLECTORS.containsKey(containerType);
    }

    @Override
    public Optional<Type> elementType(Type containerType) {
        return containerType instanceof Class ? Optional.of(ELEMENT_TYPES.get(containerType)) : Optional.empty();
    }

    @Override
    public Collector<?, ?, ?> build(Type containerType) {
        return containerType instanceof Class ? COLLECTORS.get(containerType) : null;
    }
}
