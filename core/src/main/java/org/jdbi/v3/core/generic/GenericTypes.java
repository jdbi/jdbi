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
package org.jdbi.v3.core.generic;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.Optional;
import org.jdbi.v3.lib.internal.guava.com.google.common.reflect.TypeParameter;
import org.jdbi.v3.lib.internal.guava.com.google.common.reflect.TypeToken;

/**
 * Utilities for working with generic types.
 */
@SuppressWarnings("rawtypes")
public class GenericTypes {
    private static final TypeVariable<Class<Map>> KEY;
    private static final TypeVariable<Class<Map>> VALUE;

    static {
        TypeVariable<Class<Map>>[] mapParams = Map.class.getTypeParameters();
        KEY = mapParams[0];
        VALUE = mapParams[1];
    }

    private GenericTypes() {
        throw new UnsupportedOperationException("utility class");
    }

    /**
     * Returns the erased class for the given type.
     *
     * <p>
     * Example: if type is <code>List&lt;String&gt;</code>, returns
     * <code>List.class</code>
     * </p>
     * parameter
     *
     * @param type
     *            the type
     * @return the erased class
     */
    public static Class<?> getErasedType(Type type) {
        return TypeToken.of(type).getRawType();
    }

    /**
     * Same as {@link #findGenericParameter(Type, Class, int)} with n = 0
     */
    public static Optional<Type> findGenericParameter(Type type, Class<?> parameterizedSupertype) {
        return findGenericParameter(type, parameterizedSupertype, 0);
    }

    /**
     * For the given type which extends parameterizedSupertype, returns the
     * nth generic parameter for the parameterized supertype, if concretely
     * expressed.
     *
     * <p>
     * Example:
     * </p>
     * <ul>
     * <li>if <code>type</code> is <code>ArrayList&lt;String&gt;</code>,
     * <code>parameterizedSuperType</code> is <code>List.class</code>,
     * and <code>n</code> is <code>0</code>,
     * returns <code>Optional.of(String.class)</code>.</li>
     * <li>if <code>type</code> is <code>Map&lt;String, Integer&gt;</code>,
     * <code>parameterizedSuperType</code> is <code>Map.class</code>,
     * and <code>n</code> is <code>1</code>,
     * returns <code>Optional.of(Integer.class)</code>.</li>
     * <li>if <code>type</code> is <code>ArrayList.class</code> (raw),
     * <code>parameterizedSuperType</code> is <code>List.class</code>,
     * and <code>n</code> is <code>0</code>,
     * returns <code>Optional.empty()</code>.</li>
     * </ul>
     *
     * @param type the subtype of parameterizedSupertype
     * @param parameterizedSupertype the parameterized supertype from which we want the generic
     *            parameter
     * @param n the index in <code>Foo&lt;X, Y, Z, ...&gt;</code>
     * @return the parameter on the supertype, if it is concretely defined.
     * @throws ArrayIndexOutOfBoundsException if n &gt; the number of type variables the type has
     */
    public static Optional<Type> findGenericParameter(Type type, Class<?> parameterizedSupertype, int n) {
        Type parameterType = resolveType(parameterizedSupertype.getTypeParameters()[n], type);
        return parameterType instanceof Class || parameterType instanceof ParameterizedType
            ? Optional.of(parameterType)
            : Optional.empty();
    }

    /**
     * Resolves the <code>type</code> parameter in the context of <code>contextType</code>. For example, if
     * <code>type</code> is <code>List.class.getMethod("get", int.class).getGenericReturnType()</code>, and
     * <code>contextType</code> is <code>List&lt;String&gt;</code>, this method returns <code>String.class</code>
     * @param type the type to be resolved in the scope of <code>contextType</code>
     * @param contextType the context type in which <code>type</code> is interpreted to resolve the type.
     * @return the resolved type.
     */
    public static Type resolveType(Type type, Type contextType) {
        return TypeToken.of(contextType)
                .resolveType(type)
                .getType();
    }

    /**
     * @param type a type
     * @return whether the given {@code Type} is an Array type.
     */
    public static boolean isArray(Type type) {
        return type instanceof Class<?> && ((Class<?>) type).isArray();
    }

    /**
     * Given a subtype of {@code Map<K,V>}, returns the corresponding map entry type {@code Map.Entry<K,V>}.
     * @param mapType the map subtype
     * @return the map entry type
     */
    public static Type resolveMapEntryType(Type mapType) {
        Type keyType = resolveType(KEY, mapType);
        Type valueType = resolveType(VALUE, mapType);
        return resolveMapEntryType(keyType, valueType);
    }

    /**
     * Given a key and value type, returns the map entry type {@code Map.Entry<keyType,valueType>}.
     * @param keyType the key type
     * @param valueType the value type
     * @return the map entry type
     */
    public static Type resolveMapEntryType(Type keyType, Type valueType) {
        return resolveMapEntryType(TypeToken.of(keyType), TypeToken.of(valueType));
    }

    private static <K, V> Type resolveMapEntryType(TypeToken<K> keyType, TypeToken<V> valueType) {
        return new TypeToken<Map.Entry<K, V>>() {}
                .where(new TypeParameter<K>() {}, keyType)
                .where(new TypeParameter<V>() {}, valueType)
                .getType();
    }
}
