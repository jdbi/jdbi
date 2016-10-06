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

package org.jdbi.v3.sqlobject.customizers;


import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

/**
 * Used to dynamically set {@link List} arguments as attributes on the StatementContext for the statement generated for
 * this method.
 * <p>
 * A {@code List} argument passed to {@code @DefineIn} will be converted to a comma-separated String and set as a whole
 * as a single specified attribute. Null members in the {@code List} are ignored, but duplicate members in the
 * {@code List} may cause SQL exceptions. An empty {@code List} will result in an {@link IllegalArgumentException}.
 *
 * @see Define
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@SqlStatementCustomizingAnnotation(DefineIn.Factory.class)
@interface DefineIn
{
    /**
     * The key for the attribute to set. The value will be the value passed to the annotated argument
     *
     * @return the attribute key
     */
    String value() default "";

    final class Factory implements SqlStatementCustomizerFactory
    {

        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation, Class<?> sqlObjectType, Method method, Parameter param, final Object arg)
        {
            if (!(arg instanceof List)) {
                if (arg == null) {
                    throw new IllegalArgumentException("A null object was passed as a @DefineIn parameter. @DefineIn " +
                            "is only supported on List arguments");
                }
                throw new IllegalArgumentException("A " + arg.getClass() + " object was passed as a @DefineIn " +
                        "parameter. @DefineIn is only supported on List arguments");
            }
            List<?> argsList = (List<?>) arg;
            if (argsList.isEmpty()) {
                throw new IllegalArgumentException("An empty list was passed as a @DefineIn parameter. Can't define" +
                        " an empty attribute.");
            }

            DefineIn d = (DefineIn) annotation;
            String name = d.value();
            if (name.isEmpty()) {
                if (param.isNamePresent()) {
                    name = param.getName();
                } else {
                    throw new UnsupportedOperationException("A @DefineIn parameter was not given a name, "
                            + "and parameter name data is not present in the class file, for: "
                            + param.getDeclaringExecutable() + " :: " + param);
                }
            }
            final String key = name;

            StringBuilder sb = new StringBuilder();
            boolean firstItem = true;
            for (Object o : argsList) {
                if (o == null) {
                    continue;
                }
                if (firstItem) {
                    firstItem = false;
                } else {
                    sb.append(',');
                }
                sb.append(o.toString());
            }
            return q -> q.define(key, sb.toString());
        }
    }
}
