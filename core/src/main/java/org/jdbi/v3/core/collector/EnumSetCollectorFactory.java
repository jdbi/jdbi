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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Collector;

import static org.jdbi.v3.core.generic.GenericTypes.findGenericParameter;
import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

class EnumSetCollectorFactory implements CollectorFactory {

    @Override
    public boolean accepts(Type containerType) {
        // elements of EnumSet must always be enums, no need to check
        return EnumSet.class.isAssignableFrom(getErasedType(containerType))
            && containerType instanceof ParameterizedType;
    }

    @Override
    public Optional<Type> elementType(Type containerType) {
        return findGenericParameter(containerType, getErasedType(containerType));
    }

    @Override
    public Collector<?, ?, ?> build(Type containerType) {
        Class<? extends Enum> componentType = getComponentClass(containerType);
        return Collector.of(
                () -> new EnumSetBuilder(componentType),
                EnumSetBuilder::add,
                EnumSetBuilder::combine,
                EnumSetBuilder::build);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Enum> getComponentClass(Type containerType) {
        return findGenericParameter(containerType, getErasedType(containerType))
            .map(type -> {
                Class<? extends Enum> componentClass = null;
                if (type instanceof Class) {
                    componentClass = (Class<? extends Enum>) type;
                } else if (type instanceof ParameterizedType) {
                    componentClass = (Class<? extends Enum>) ((ParameterizedType) type).getRawType();
                }

                return componentClass;
            })
            .orElseThrow(() ->
                new IllegalArgumentException("cannot determine class of type " + containerType.getTypeName()
                    + " represented by " + containerType.getClass().getSimpleName()));
    }
}
