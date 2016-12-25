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

import org.jdbi.v3.sqlobject.internal.ParameterUtil;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Binds each property for each value in the annotated {@link Iterable} or array/varargs argument,
 * and defines a named attribute as a comma-separated list of each bound parameter name.
 *
 * Used to create query similar to:
 * select * from things where (id, name) in ((1,'abc'),(2,'def'),(3,'ghi'))
 *
 * <p>
 * <pre>
 * &#64;SqlQuery("select * from things where (id, name) in (&lt;keys&gt;)")
 * List&lt;Thing&gt; getThings(@BindBeanList(value = "keys", propertyNames = {"id", "name"}) ThingKey... thingKeys)
 * </pre>
 * <p>
 * <p>
 * Throws IllegalArgumentException if the argument is not an array or Iterable.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@SqlStatementCustomizingAnnotation(BindBeanList.Factory.class)
public @interface BindBeanList {
    /**
     * The attribute name to define. If omitted, the name of the annotated parameter is used. It is an error to omit
     * the name when there is no parameter naming information in your class files.
     *
     * @return the attribute name.
     */
    String value() default "";

    /**
     * The list of properties to invoke on each element in the argument
     *
     * @return the property names
     */
    String[] propertyNames();

    final class Factory implements SqlStatementCustomizerFactory {
        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation,
                                                         Class<?> sqlObjectType,
                                                         Method method,
                                                         Parameter param,
                                                         int index,
                                                         Object arg) {
            final BindBeanList bindBeanList = (BindBeanList) annotation;
            final String name = ParameterUtil.getParameterName(bindBeanList, bindBeanList.value(), param);

            if (arg == null) {
                throw new IllegalArgumentException("argument is null; null was explicitly forbidden on BindBeanList");
            }

            List<Object> list = new ArrayList<>();
            for (Iterator<?> iter = BindListUtil.toIterator(arg); iter.hasNext(); ) {
                list.add(iter.next());
            }

            return stmt -> stmt.bindBeanList(name, list, Arrays.asList(bindBeanList.propertyNames()));
        }

    }
}
