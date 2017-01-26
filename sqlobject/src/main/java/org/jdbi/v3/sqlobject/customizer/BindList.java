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

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.jdbi.v3.core.internal.IterableLike;
import org.jdbi.v3.core.statement.SqlStatement;
import org.jdbi.v3.sqlobject.internal.ParameterUtil;

/**
 * Binds each value in the annotated {@link Iterable} or array/varargs argument, and defines a named attribute as a
 * comma-separated list of each bound parameter name. Common use cases:
 * <p>
 * <pre>
 * &#64;SqlQuery("select * from things where id in (&lt;ids&gt;)")
 * List&lt;Thing&gt; getThings(@BindList int... ids)
 *
 * &#64;SqlQuery("insert into things (&lt;columnNames&gt;) values (&lt;values&gt;)")
 * void insertThings(@DefineList List&lt;String&gt; columnNames, @BindList List&lt;Object&gt; values)
 * </pre>
 * <p>
 * <p>
 * Throws IllegalArgumentException if the argument is not an array or Iterable. How null and empty collections are handled can be configured with onEmpty:EmptyHandling - throws IllegalArgumentException by default.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@SqlStatementCustomizingAnnotation(BindList.Factory.class)
public @interface BindList {
    /**
     * The attribute name to define. If omitted, the name of the annotated parameter is used. It is an error to omit
     * the name when there is no parameter naming information in your class files.
     *
     * @return the attribute name.
     */
    String value() default "";

    /**
     * what to do when the argument is null or empty
     */
    EmptyHandling onEmpty() default EmptyHandling.THROW;

    final class Factory implements SqlStatementCustomizer {
        @Override
        public void customizeForParameter(SqlStatement<?> statement,
                                          Annotation annotation,
                                          Class<?> sqlObjectType,
                                          Method method,
                                          Parameter param,
                                          int index,
                                          Object arg) {
            final BindList bindList = (BindList) annotation;
            final String name = ParameterUtil.getParameterName(bindList, bindList.value(), param);

            if (arg == null || IterableLike.isEmpty(arg)) {
                switch (bindList.onEmpty()) {
                    case VOID:
                        statement.define(name, "");
                        return;
                    case NULL:
                        statement.define(name, "null");
                        return;
                    case THROW:
                        throw new IllegalArgumentException(arg == null
                                ? "argument is null; null was explicitly forbidden on this instance of BindList"
                                : "argument is empty; emptiness was explicitly forbidden on this instance of BindList");
                    default:
                        throw new IllegalStateException(EmptyHandling.valueNotHandledMessage);
                }
            }

            statement.bindList(name, IterableLike.toList(arg));
        }

    }

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

        static final String valueNotHandledMessage = "EmptyHandling type on BindList not handled. Please report this to the jdbi developers.";
    }
}
