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
package org.jdbi.v3;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import com.google.common.reflect.TypeToken;

public class Types {
    /**
     * Returns the erased class for the given type.
     *
     * <p>
     * Example: if type is <code>List&lt;String></code>, returns
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
     * <ul>
     * <li>if <code>type</code> is <code>ArrayList&lt;String></code> and
     * <code>parameterizedSuperType</code> is <code>List.class</code>, returns
     * <code>Optional.of(String.class)</code>.</li>
     * <li>if <code>type</code> is <code>ArrayList.class</code> (raw) and
     * <code>parameterizedSuperType</code> is <code>List.class</code>, returns
     * <code>Optional.empty()</code>.</li>
     * </ul>
     *
     * </p>
     *
     * @param type
     *            the subtype of parameterizedSupertype
     * @param parameterizedSupertype
     *            the parameterized supertype from which we want the generic
     *            parameter
     * @return the parameter on the supertype, if it is concretely defined.
     */
    public static Optional<Type> findGenericParameter(Type type, Class<?> parameterizedSupertype) {
        Type parameterType = TypeToken.of(type)
                .resolveType(parameterizedSupertype.getTypeParameters()[0])
                .getType();
        if (parameterType instanceof Class || parameterType instanceof ParameterizedType) {
            return Optional.of(parameterType);
        }
        return Optional.empty();
    }
}
