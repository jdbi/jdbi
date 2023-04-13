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
import java.util.Optional;
import java.util.stream.Collector;

import org.jdbi.v3.core.generic.GenericTypes;

class SimpleCollectorFactory implements CollectorFactory {
    private final Type containerType;
    private final Collector<?, ?, ?> collector;

    SimpleCollectorFactory(Type containerType, Collector<?, ?, ?> collector) {
        this.containerType = containerType;
        this.collector = collector;
    }

    @Override
    public boolean accepts(Type containerType) {
        return GenericTypes.isSuperType(containerType, this.containerType);
    }

    @Override
    public Optional<Type> elementType(Type containerType) {
        if (GenericTypes.isSuperType(Iterable.class, containerType)) {
            return GenericTypes.findGenericParameter(containerType, Iterable.class);
        }
        return Optional.empty();
    }

    @Override
    public Collector<?, ?, ?> build(Type containerType) {
        return collector;
    }

    @Override
    public String toString() {
        return "CollectorFactory handling " + containerType + " with " + collector;
    }
}
