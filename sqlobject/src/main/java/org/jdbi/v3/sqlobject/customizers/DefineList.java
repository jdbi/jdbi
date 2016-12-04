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
import org.jdbi.v3.sqlobject.internal.ParameterUtil;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

/**
 * Defines a named attribute as a comma-separated {@link String} from the elements of the annotated array or
 * {@link List} argument. Attributes are stored on the {@link org.jdbi.v3.core.StatementContext}, and may be used by
 * statement customizers such as the statement rewriter. For example:
 *
 * <pre>
 * &#64;SqlUpdate("insert into &lt;table&gt; (&lt;columns&gt;) values (&lt;values&gt;)")
 * int insert(@Define String table, @DefineList List&lt;String&gt; columns, @BindList List&lt;Object&gt; values);
 *
 * &#64;SqlQuery("select &lt;columns&gt; from &lt;table&gt; where id = :id")
 * ResultSet select(@DefineList("columns") List&lt;String&gt; columns, @Define("table") String table, @Bind("id") long id);
 * </pre>
 *
 * <p>
 * An array or {@code List} argument passed to {@code @DefineList} will be converted to a comma-separated String and set
 * as a whole as a single specified attribute. Duplicate members in the {@code List} may cause SQL exceptions. An empty
 * {@code List} or {@code null} members in the {@code List} will result in an {@link IllegalArgumentException}.
 * </p>
 *
 * <p>
 * Be aware of the list members you're binding with @DefineList, as there is no input sanitization! <b>Blindly passing
 * Strings through <code>@DefineList</code> may make your application vulnerable to SQL Injection.</b>
 * </p>
 *
 * @see Define
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@SqlStatementCustomizingAnnotation(DefineList.Factory.class)
public @interface DefineList
{
    /**
     * The attribute name to define. If omitted, the name of the annotated parameter is used. It is an error to omit
     * the name when there is no parameter naming information in your class files.
     *
     * @return the attribute key
     */
    String value() default "";

    final class Factory implements SqlStatementCustomizerFactory
    {

        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation,
                                                         Class<?> sqlObjectType,
                                                         Method method,
                                                         Parameter param,
                                                         int index,
                                                         Object arg)
        {
            List<?> argsList;
            if (arg instanceof List) {
                argsList = (List<?>) arg;
            } else if (arg instanceof Object[]) {
                argsList = Arrays.asList((Object[]) arg);
            } else {
                if (arg == null) {
                    throw new IllegalArgumentException("A null object was passed as a @DefineList parameter. " +
                            "@DefineList is only supported on List and array arguments");
                }
                throw new IllegalArgumentException("A " + arg.getClass() + " object was passed as a @DefineList " +
                        "parameter. @DefineList is only supported on List and array arguments");
            }
            if (argsList.isEmpty()) {
                throw new IllegalArgumentException("An empty list was passed as a @DefineList parameter. Can't define " +
                        "an empty attribute.");
            }
            if (argsList.contains(null)) {
                throw new IllegalArgumentException("A @DefineList parameter was passed a list with null values in it.");
            }

            DefineList d = (DefineList) annotation;
            final String name = ParameterUtil.getParameterName(d, d.value(), param);

            return stmt -> stmt.defineList(name, argsList);
        }
    }
}
