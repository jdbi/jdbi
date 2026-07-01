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
package org.jdbi.v3.sqlobject.statement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdbi.v3.meta.Alpha;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.sqlobject.statement.internal.SqlPreflightFactory;

/**
 * Executes a "pre-flight" SQL statement on the same {@code Handle}, immediately before the
 * annotated SQL Object method's main statement runs. The primary use case is setting a
 * scoped/session variable before a query, for example a non-default Postgres trigram threshold:
 *
 * <pre>
 * &#064;SqlPreflight("set pg_trgm.word_similarity_threshold=0.4")
 * &#064;SqlQuery("find-user")
 * &#064;RegisterFieldMapper(User.class)
 * List&lt;User&gt; findUser(String search);
 * </pre>
 *
 * <p>The preflight statement runs during the main statement's setup, on the same {@code Handle}, so
 * it always shares the main statement's connection and transaction. In particular, if the method is
 * annotated with {@code @Transaction}, the preflight runs inside that transaction regardless of the
 * order in which the two annotations are declared. The method's arguments are bound to the preflight
 * statement, respecting parameter binding annotations such as {@code @Bind} and {@code @BindBean};
 * arguments that the preflight SQL does not reference are ignored.
 *
 * <p>The annotation is repeatable; multiple preflight statements run in declaration order. It may be
 * placed on a method or on the SQL Object type (type-level preflights apply to all methods, and run
 * before any method-level preflights).
 *
 * <p>The value is always used as literal SQL; unlike {@code @SqlQuery} and {@code @SqlUpdate}, it is
 * not resolved through the configured {@code SqlLocator}, so external {@code .sql} files have no
 * effect on it.
 *
 * @since 3.54.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@SqlStatementCustomizingAnnotation(SqlPreflightFactory.class)
@Repeatable(SqlPreflights.class)
@Alpha
public @interface SqlPreflight {

    /**
     * Returns the literal SQL to execute before the main statement.
     *
     * @return the preflight SQL string
     */
    String value();
}
