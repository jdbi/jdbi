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

import org.jdbi.v3.sqlobject.customizer.internal.BindListFactory;

/**
 * Binds each value in the annotated {@link Iterable} or array/varargs argument, and defines a named attribute as a
 * comma-separated list of each bound parameter name. Common use cases:
 * <pre>
 * &#64;SqlQuery("select * from things where id in (&lt;ids&gt;)")
 * List&lt;Thing&gt; getThings(@BindList int... ids)
 *
 * &#64;SqlQuery("insert into things (&lt;columnNames&gt;) values (&lt;values&gt;)")
 * void insertThings(@DefineList List&lt;String&gt; columnNames, @BindList List&lt;Object&gt; values)
 * </pre>
 * <p>
 * Throws IllegalArgumentException if the argument is not an array or Iterable. How null and empty collections are handled can be configured with onEmpty:EmptyHandling - throws IllegalArgumentException by default.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@SqlStatementCustomizingAnnotation(BindListFactory.class)
public @interface BindList {
    /**
     * The attribute name to define. If omitted, the name of the annotated parameter is used. It is an error to omit
     * the name when there is no parameter naming information in your class files.
     *
     * @return the attribute name.
     */
    String value() default "";

    /**
     * @return what to do when the argument is null or empty
     */
    EmptyHandling onEmpty() default EmptyHandling.THROW;

    /**
     * describes what needs to be done if the passed argument is null or empty
     */
    enum EmptyHandling {
        /**
         * output "" (without quotes, i.e. nothing)
         * <p>
         * select * from things where x in ()
         */
        VOID,
        /**
         * output "null" (without quotes, as keyword), useful e.g. in postgresql where "in ()" is invalid syntax
         * <p>
         * select * from things where x in (null)
         */
        NULL,
        /**
         * throw IllegalArgumentException
         */
        THROW;
    }
}
