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
package org.jdbi.v3.vavr;

import io.vavr.Tuple2;
import io.vavr.collection.Map;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.generic.internal.TypeParameter;
import org.jdbi.v3.core.generic.internal.TypeToken;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * a helper similar to {@link org.jdbi.v3.core.generic.GenericTypes} but for Vavr Maps
 */
@SuppressWarnings("rawtypes")
class VavrGenericMapUtil {

    private static final TypeVariable<Class<Map>> KEY;
    private static final TypeVariable<Class<Map>> VALUE;

    static {
        TypeVariable<Class<Map>>[] mapParams = Map.class.getTypeParameters();
        KEY = mapParams[0];
        VALUE = mapParams[1];
    }

    static Type resolveMapEntryType(Type mapType) {
        Type keyType = GenericTypes.resolveType(KEY, mapType);
        Type valueType = GenericTypes.resolveType(VALUE, mapType);
        return resolveMapEntryType(keyType, valueType);
    }

    private static Type resolveMapEntryType(Type keyType, Type valueType) {
        return resolveMapEntryType(TypeToken.of(keyType), TypeToken.of(valueType));
    }

    private static <K, V> Type resolveMapEntryType(TypeToken<K> keyType, TypeToken<V> valueType) {
        return new TypeToken<Tuple2<K, V>>() {}
                .where(new TypeParameter<K>() {}, keyType)
                .where(new TypeParameter<V>() {}, valueType)
                .getType();
    }
}
