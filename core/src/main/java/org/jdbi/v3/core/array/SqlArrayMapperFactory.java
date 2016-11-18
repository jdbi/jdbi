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
package org.jdbi.v3.core.array;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import org.jdbi.v3.core.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.util.GenericTypes;

public class SqlArrayMapperFactory implements ColumnMapperFactory {
    private final Map<Class<?>, Supplier<Collection<?>>> suppliers = new HashMap<>();

    public SqlArrayMapperFactory() {
        suppliers.put(List.class, ArrayList::new);
        suppliers.put(ArrayList.class, ArrayList::new);
        suppliers.put(LinkedList.class, LinkedList::new);
        suppliers.put(CopyOnWriteArrayList.class, CopyOnWriteArrayList::new);

        suppliers.put(Set.class, HashSet::new);
        suppliers.put(HashSet.class, HashSet::new);
        suppliers.put(LinkedHashSet.class, LinkedHashSet::new);
        suppliers.put(TreeSet.class, TreeSet::new);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        final Class<?> erasedType = GenericTypes.getErasedType(type);

        if (erasedType.isArray()) {
            Class<?> elementType = erasedType.getComponentType();
            return elementTypeMapper(elementType, config)
                    .map(elementMapper -> new ArrayColumnMapper(elementMapper, elementType));
        }

        Supplier<Collection<?>> supplier = suppliers.get(erasedType);
        if (supplier != null) {
            return GenericTypes.findGenericParameter(type, Collection.class)
                    .flatMap(elementType -> elementTypeMapper(elementType, config))
                    .map(elementMapper -> new CollectionColumnMapper(elementMapper, supplier));
        }

        return Optional.empty();
    }

    private Optional<ColumnMapper<?>> elementTypeMapper(Type elementType, ConfigRegistry config) {
        Optional<ColumnMapper<?>> mapper = config.get(ColumnMappers.class).findFor(elementType, config);

        if (!mapper.isPresent() && elementType == Object.class) {
            return Optional.of((rs, num, context) -> rs.getObject(num));
        }

        return mapper;
    }
}
