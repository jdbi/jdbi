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

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.generic.internal.TypeParameter;
import org.jdbi.v3.core.generic.internal.TypeToken;
import org.jdbi.v3.meta.Beta;

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
     * For the given type which extends parameterizedSupertype, returns the
     * first generic parameter for parameterized supertype, if concretely
     * expressed.
     *
     * <p>
     * Example:
     * </p>
     * <ul>
     * <li>if <code>type</code> is <code>ArrayList&lt;String&gt;</code> and
     * <code>parameterizedSuperType</code> is <code>List.class</code>, returns
     * <code>Optional.of(String.class)</code>.</li>
     * <li>if <code>type</code> is <code>ArrayList.class</code> (raw) and
     * <code>parameterizedSuperType</code> is <code>List.class</code>, returns
     * <code>Optional.empty()</code>.</li>
     * </ul>
     *
     * @param type
     *            the subtype of parameterizedSupertype
     * @param parameterizedSupertype
     *            the parameterized supertype from which we want the generic
     *            parameter
     * @return the parameter on the supertype, if it is concretely defined.
     */
    public static Optional<Type> findGenericParameter(Type type, Class<?> parameterizedSupertype) {
        Type parameterType = resolveType(parameterizedSupertype.getTypeParameters()[0], type);
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

    /**
     * Given a class that extends a generic superclass, and a parameter index, return the generic parameter at that
     * index from the {@code extends} clause in the class declaration.
     *
     * @param clazz the class from which to extract a generic parameter
     * @param parameterIndex the index of the parameter to return
     * @return the annotated type parameter
     */
    @Beta
    public static Optional<AnnotatedType> findSuperclassAnnotatedTypeParameter(Class<?> clazz, int parameterIndex) {
        AnnotatedType annotatedSuperclass = clazz.getAnnotatedSuperclass();

        if (annotatedSuperclass instanceof AnnotatedParameterizedType) {
            AnnotatedParameterizedType annotatedParameterizedSuperclass = (AnnotatedParameterizedType) annotatedSuperclass;
            return Optional.of(annotatedParameterizedSuperclass.getAnnotatedActualTypeArguments()[parameterIndex]);
        }

        return Optional.empty();
    }

    /**
     * Given a class, a generic interface the class extends, and a parameter index, return the annotated generic
     * parameter at that index from the {@code implements} clause in the class declaration.
     *
     * @param clazz the class from which to extract a generic parameter
     * @param parameterIndex the index of the parameter to return
     * @return the annotated type parameter
     */
    @Beta
    public static Optional<AnnotatedType> findInterfaceAnnotatedTypeParameter(Class<?> clazz, Class<?> implementedInterface, int parameterIndex) {
        return Arrays.stream(clazz.getAnnotatedInterfaces())
            .filter(annotatedIface -> implementedInterface.equals(getErasedType(annotatedIface.getType())))
            .filter(AnnotatedParameterizedType.class::isInstance)
            .map(AnnotatedParameterizedType.class::cast)
            .map(annotatedParameterizedIface -> annotatedParameterizedIface.getAnnotatedActualTypeArguments()[parameterIndex])
            .findFirst();
    }
}
