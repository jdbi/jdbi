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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdbi.v3.meta.Beta;

/**
 * <p>Annotate a SqlObject type, method, or field as {@code @Definition} in order to
 * {@code define} an attribute for all SqlObject methods in the same type.</p>
 *
 * <ul>
 * <li>On a type, both the {@code key} and {@code value} must be specified.</li>
 * <li>On a method, the {@code key} defaults to the method name,
 * and the {@code value} will be the result of calling the method statically.</li>
 * <li>On a field, the {@code key} defaults to the field name,
 * and the {@code value} will be the result of getting the field value statically.</li>
 * </ul>
 */
@Target({
    ElementType.FIELD,
    ElementType.METHOD,
    ElementType.TYPE
})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Definitions.class)
@Beta
public @interface Definition {
    String UNDEFINED = "__definition_undefined__";
    String key() default UNDEFINED;
    String value() default UNDEFINED;
}
