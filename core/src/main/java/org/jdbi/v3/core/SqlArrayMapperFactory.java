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
package org.jdbi.v3.core;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.util.GenericTypes;

class SqlArrayMapperFactory implements ColumnMapperFactory {
    private final Map<Class<?>, Supplier<List<?>>> listSuppliers = new HashMap<>();

    SqlArrayMapperFactory() {
        listSuppliers.put(List.class, ArrayList::new);
        listSuppliers.put(ArrayList.class, ArrayList::new);
        listSuppliers.put(LinkedList.class, LinkedList::new);
        listSuppliers.put(CopyOnWriteArrayList.class, CopyOnWriteArrayList::new);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<ColumnMapper<?>> build(Type type, StatementContext ctx) {
        final Class<?> erasedType = GenericTypes.getErasedType(type);

        if (erasedType.isArray()) {
            Class<?> elementType = erasedType.getComponentType();
            return elementTypeMapper(elementType, ctx)
                    .map(elementMapper -> new ArrayColumnMapper(elementMapper, elementType));
        }

        Supplier<List<?>> supplier = listSuppliers.get(erasedType);
        if (supplier != null) {
            return GenericTypes.findGenericParameter(type, List.class)
                    .flatMap(elementType -> elementTypeMapper(elementType, ctx))
                    .map(elementMapper -> new ListColumnMapper(elementMapper, supplier));
        }

        return Optional.empty();
    }

    private Optional<ColumnMapper<?>> elementTypeMapper(Type elementType, StatementContext ctx) {
        Optional<ColumnMapper<?>> mapper = ctx.getConfig(MappingRegistry.class).findColumnMapperFor(elementType, ctx);

        if (!mapper.isPresent() && elementType == Object.class) {
            return Optional.of((rs, num, context) -> rs.getObject(num));
        }

        return mapper;
    }
}
