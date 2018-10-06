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

import org.jdbi.v3.core.generic.GenericTypes;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;

import static org.jdbi.v3.core.generic.GenericTypes.findGenericParameter;
import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

class EnumSetCollectorFactory implements CollectorFactory {

    @Override
    public boolean accepts(Type containerType) {
        // compiler ensures that elements of EnumSet<E extends Enum<E>> are always enums
        return EnumSet.class.isAssignableFrom(getErasedType(containerType))
            && containerType instanceof ParameterizedType;
    }

    @Override
    public Optional<Type> elementType(Type containerType) {
        return findGenericParameter(containerType, EnumSet.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collector<?, ?, ?> build(Type containerType) {
        Class<? extends Enum> componentType = (Class<? extends Enum>) findGenericParameter(containerType, EnumSet.class)
            .map(GenericTypes::getErasedType)
            .orElseThrow(() -> new IllegalStateException("Cannot determine EnumSet element type"));

        return Collector.of(
                () -> EnumSet.noneOf(componentType),
                EnumSet::add,
                (left, right) -> {
                    if (left.size() < right.size()) {
                        right.addAll(left);
                        return right;
                    } else {
                        left.addAll(right);
                        return left;
                    }
                },
                Function.identity());
    }
}
