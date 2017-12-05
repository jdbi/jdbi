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

import static org.jdbi.v3.core.collector.OptionalCollectors.toOptionalDouble;
import static org.jdbi.v3.core.collector.OptionalCollectors.toOptionalInt;
import static org.jdbi.v3.core.collector.OptionalCollectors.toOptionalLong;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Collector;

class OptionalPrimitiveCollectorFactory implements CollectorFactory {
    static final Map<Class<?>, Collector<?, ?, ?>> collectors = new HashMap<>();
    static final Map<Class<?>, Class<?>> elementTypes = new HashMap<>();

    {
        collectors.put(OptionalInt.class, toOptionalInt());
        elementTypes.put(OptionalInt.class, Integer.class);

        collectors.put(OptionalLong.class, toOptionalLong());
        elementTypes.put(OptionalLong.class, Long.class);

        collectors.put(OptionalDouble.class, toOptionalDouble());
        elementTypes.put(OptionalDouble.class, Double.class);
    }

    @Override
    public boolean accepts(Type containerType) {
        return collectors.containsKey(containerType);
    }

    @Override
    public Optional<Type> elementType(Type containerType) {
        return Optional.of(elementTypes.get(containerType));
    }

    @Override
    public Collector<?, ?, ?> build(Type containerType) {
        return collectors.get(containerType);
    }
}
