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
package org.jdbi.v3.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import com.google.common.reflect.TypeToken;

public class Types {
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
}
