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
package org.jdbi.v3.sqlobject.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Types;

import org.jdbi.v3.sqlobject.config.internal.RegisterObjectArgumentFactoryImpl;

/**
 * Used to register argument factories for types which are compatible with
 * {@link java.sql.PreparedStatement#setObject(int, Object)}.
 */
@ConfiguringAnnotation(RegisterObjectArgumentFactoryImpl.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterObjectArgumentFactory
{
    /**
     * The argument type(s) which are compatible with {@link java.sql.PreparedStatement#setObject(int, Object)}.
     * @return the argument types
     */
    Class<?>[] value();

    /**
     * SQL type constant(s) from {@link Types}. If omitted, defaults to not setting a type. If specified, must have the
     * same number of elements as {@link #value()}. Each <code>sqlType</code> element is applied to the
     * <code>value</code> element at the same index.
     * @return SQL types corresponding pairwise to the elements in {@link #value()}.
     */
    int[] sqlType() default {};
}
