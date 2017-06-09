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
package org.jdbi.v3.stringtemplate4;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdbi.v3.core.rewriter.ColonPrefixStatementRewriter;
import org.jdbi.v3.core.rewriter.StatementRewriter;
import org.jdbi.v3.sqlobject.config.ConfiguringAnnotation;
import org.jdbi.v3.stringtemplate4.internal.UseStringTemplateSqlLocatorImpl;

/**
 * Configures SQL Object to locate SQL using the {@link StringTemplateSqlLocator#findStringTemplate(Class, String)}
 * method. If the SQL annotation (e.g. <code>@SqlQuery</code>) defines a value (e.g. <code>@SqlQuery("hello")</code>),
 * that value (<code>"hello"</code>) will be used for the <code>name</code> parameter; if undefined, the name of the SQL
 * object method will be used:
 *
 * <pre>
 *     &#064;UseStringTemplateSqlLocator
 *     interface Viccini {
 *         &#064;SqlUpdate
 *         void doTheThing(long id);     // =&gt; StringTemplateSqlLocator.findStringTemplateSql(Viccini.class, "doTheThing")
 *
 *         &#064;SqlUpdate("thatOtherThing")
 *         void doTheThing(String name); // =&gt; StringTemplateSqlLocator.findStringTemplateSql(Viccini.class, "thatOtherThing")
 *     }
 * </pre>
 */
@ConfiguringAnnotation(UseStringTemplateSqlLocatorImpl.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface UseStringTemplateSqlLocator {
    Class<? extends StatementRewriter> value() default ColonPrefixStatementRewriter.class;
}
