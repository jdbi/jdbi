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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;

import io.vavr.Tuple2;
import io.vavr.collection.Map;
import io.vavr.collection.Multimap;
import org.jdbi.v3.core.generic.GenericTypes;

/**
 * a helper similar to {@link org.jdbi.v3.core.generic.GenericTypes} but for Vavr Maps
 */
@SuppressWarnings("rawtypes")
class VavrGenericMapUtil {

    private static final TypeVariable<Class<Map>> KEY;
    private static final TypeVariable<Class<Map>> VALUE;

    private static final TypeVariable<Class<Multimap>> KEY_MULTIMAP;
    private static final TypeVariable<Class<Multimap>> VALUE_MULTIMAP;

    static {
        TypeVariable<Class<Map>>[] mapParams = Map.class.getTypeParameters();
        KEY = mapParams[0];
        VALUE = mapParams[1];

        TypeVariable<Class<Multimap>>[] multimapParams = Multimap.class.getTypeParameters();
        KEY_MULTIMAP = multimapParams[0];
        VALUE_MULTIMAP = multimapParams[1];
    }

    private VavrGenericMapUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    static Type resolveMapEntryType(Type mapType) {
        return resolveMaplikeEntryType(mapType, KEY, VALUE);
    }

    static Type resolveMultimapEntryType(Type mapType) {
        return resolveMaplikeEntryType(mapType, KEY_MULTIMAP, VALUE_MULTIMAP);
    }

    private static Type resolveMaplikeEntryType(Type maplikeType, Type keyParam, Type valueParam) {
        Type keyType = GenericTypes.resolveType(keyParam, maplikeType);
        Type valueType = GenericTypes.resolveType(valueParam, maplikeType);
        return resolveMapEntryType(keyType, valueType);
    }

    private static Type resolveMapEntryType(Type keyType, Type valueType) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] {keyType, valueType};
            }

            @Override
            public Type getRawType() {
                return Tuple2.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof ParameterizedType)) {
                    return false;
                }
                ParameterizedType that = (ParameterizedType) obj;

                return that.getOwnerType() == null
                    && Tuple2.class.equals(that.getRawType())
                    && keyType.equals(that.getActualTypeArguments()[0])
                    && valueType.equals(that.getActualTypeArguments()[1]);
            }

            @Override
            public int hashCode() {
                return Tuple2.class.hashCode() ^ Arrays.asList(keyType, valueType).hashCode();
            }
        };
    }
}
