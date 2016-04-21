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
package org.jdbi.v3.tweak;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.stream.Collector;

/**
 * Factory for building containers of elements.
 * The collector produces only objects of the type of the container elements.
 */
public interface CollectorFactory {
    /**
     * Returns true if this factory can produce a collector for the given container type; false otherwise.
     * @param containerType the container type
     */
    boolean accepts(Type containerType);

    /**
     * Returns the container element type if it can be discovered through reflection; empty otherwise.
     * @param containerType the container type
     * @return
     */
    Optional<Type> elementType(Type containerType);

    /**
     * Supplies a {@link Collector} for the given container type.
     *
     * @param containerType the type of the container
     * @see org.jdbi.v3.StatementContext#findCollectorFor(Type)
     */
    Collector<?, ?, ?> build(Type containerType);
}
