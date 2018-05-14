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
package org.jdbi.v3.sqlobject.customizer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdbi.v3.sqlobject.customizer.internal.BindMapFactory;

/**
 * Binds the entries of a {@code Map<String, Object>} to a SQL statement.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@SqlStatementCustomizingAnnotation(BindMapFactory.class)
public @interface BindMap {
    /**
     * Prefix to apply to each map key. If specified, map keys will be bound as {@code prefix.key}.
     * @return the prefix
     */
    String value() default "";

    /**
     * The set of map keys to bind. If specified, binds only the specified keys; any keys present in this property but
     * absent from the map will be bound as {@code null}. If not specified, all map entries are bound.
     * @return the map keys to bind.
     */
    String[] keys() default {};

    /**
     * Whether to automatically convert map keys to strings.
     * If false, {@code Map} keys must be strings, or an exception is thrown.
     * If true, any object may be the key, and it will be converted with {@link Object#toString()}.
     * @return whether keys will be implicitly converted to Strings.
     */
    boolean convertKeys() default false;
}
