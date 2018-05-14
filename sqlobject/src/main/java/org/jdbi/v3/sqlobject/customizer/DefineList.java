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
import java.util.List;

import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.customizer.internal.DefineListFactory;

/**
 * Defines a named attribute as a comma-separated {@link String} from the
 * elements of the annotated array or {@link List} argument. Attributes are
 * stored on the {@link StatementContext}, and may be used by the template
 * engine. For example:
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
 * An array or {@code List} argument passed to {@code @DefineList} will be
 * converted to a comma-separated String and set as a whole as a single
 * specified attribute. Duplicate members in the {@code List} may cause SQL
 * exceptions. An empty {@code List} or {@code null} members in the
 * {@code List} will result in an {@link IllegalArgumentException}.
 * </p>
 *
 * <p>
 * Be aware of the list members you're binding with @DefineList, as there is no
 * input sanitization! <b>Blindly passing Strings through
 * <code>@DefineList</code> may make your application vulnerable to SQL
 * Injection.</b>
 * </p>
 *
 * @see Define
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@SqlStatementCustomizingAnnotation(DefineListFactory.class)
public @interface DefineList {
    /**
     * The attribute name to define. If omitted, the name of the annotated
     * parameter is used. It is an error to omit the name when there is no
     * parameter naming information in your class files.
     *
     * @return the attribute key
     */
    String value() default "";
}
