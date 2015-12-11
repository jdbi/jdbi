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
package org.jdbi.v3.sqlobject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@BindingAnnotation(BindBeanFactory.class)
public @interface BindBean
{
    String BARE_BINDING = "___jdbi_bare___";
    String value() default BARE_BINDING;

    /**
     * A type to use for resolving a bean's bindings via reflection.  If omitted, <code>argument.getClass()</code> is used.
     *
     * @return a type to use for resolving bindings as an alternative to the argument's type, or <code>Default.class</code> if
     *   type is omitted.
     */
    Class<?> type() default Default.class;

    class Default {}
}
