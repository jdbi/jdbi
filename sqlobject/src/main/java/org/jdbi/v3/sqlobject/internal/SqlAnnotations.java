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
package org.jdbi.v3.sqlobject.internal;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jdbi.v3.core.internal.JdbiOptionals;
import org.jdbi.v3.core.internal.UtilityClassException;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlCall;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlScript;
import org.jdbi.v3.sqlobject.statement.SqlScripts;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public class SqlAnnotations {
    private SqlAnnotations() {
        throw new UtilityClassException();
    }

    /**
     * Returns the <code>value()</code> of the <code>@SqlBatch</code>, <code>@SqlCall</code>, <code>@SqlQuery</code>,
     * <code>@SqlUpdate</code>, <code>@SqlScripts</code>, or <code>@SqlScript</code> annotation on the given method if declared and non-empty; empty otherwise.
     *
     * @param method the method
     * @return the annotation <code>value()</code>
     */
    public static Optional<String> getAnnotationValue(Method method) {
        return getAnnotationValue(method, Function.identity());
    }

    /**
     * Returns the <code>value()</code> of the <code>@SqlBatch</code>, <code>@SqlCall</code>, <code>@SqlQuery</code>,
     * <code>@SqlUpdate</code>, <code>@SqlScripts</code>, or <code>@SqlScript</code> annotation on the given method if declared and non-empty; empty otherwise.
     *
     * Note: <code>@SqlScripts</code> values are mapped individually and concatenated with {@code " ; "}, hence the transformation parameter.
     *
     * @param method the method
     * @param transformation the String transformation (e.g. SQL lookup) to apply to the found value(s)
     * @return the annotation <code>value()</code>
     */
    public static Optional<String> getAnnotationValue(Method method, Function<String, String> transformation) {
        Predicate<String> isNotBlank = str -> !str.trim().isEmpty();

        return JdbiOptionals.findFirstPresent(
            () -> Optional.ofNullable(method.getAnnotation(SqlBatch.class))
                .map(SqlBatch::value)
                .filter(isNotBlank)
                .map(transformation),

            () -> Optional.ofNullable(method.getAnnotation(SqlCall.class))
                .map(SqlCall::value)
                .filter(isNotBlank)
                .map(transformation),

            () -> Optional.ofNullable(method.getAnnotation(SqlQuery.class))
                .map(SqlQuery::value)
                .filter(isNotBlank)
                .map(transformation),

            () -> Optional.ofNullable(method.getAnnotation(SqlUpdate.class))
                .map(SqlUpdate::value)
                .filter(isNotBlank)
                .map(transformation),

            () -> Optional.ofNullable(method.getAnnotation(SqlScripts.class))
                .map(SqlScripts::value)
                .map(scripts -> Arrays.stream(scripts)
                    .map(SqlScript::value)
                    .filter(isNotBlank)
                    .map(transformation)
                    .collect(Collectors.joining(" ; "))),

            () -> Optional.ofNullable(method.getAnnotation(SqlScript.class))
                .map(SqlScript::value)
                .filter(isNotBlank)
                .map(transformation)
        );
    }
}
