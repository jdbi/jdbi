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
     * Returns the <code>value()</code> of the <code>@SqlBatch</code>, <code>@SqlCall</code>, <code>@SqlQuery</code>, or
     * <code>@SqlUpdate</code> annotation on the given method if declared and non-empty; empty otherwise.
     *
     * @param method the method
     * @return the annotation <code>value()</code>
     */
    public static Optional<String> getAnnotationValue(Method method, Function<String, String> resolveSql) {
        Predicate<String> nonEmpty = s -> !s.isEmpty();

        return JdbiOptionals.findFirstPresent(
                () -> Optional.ofNullable(method.getAnnotation(SqlBatch.class)).map(SqlBatch::value).map(resolveSql).filter(nonEmpty),
                () -> Optional.ofNullable(method.getAnnotation(SqlCall.class)).map(SqlCall::value).map(resolveSql).filter(nonEmpty),
                () -> Optional.ofNullable(method.getAnnotation(SqlQuery.class)).map(SqlQuery::value).map(resolveSql).filter(nonEmpty),
                () -> Optional.ofNullable(method.getAnnotation(SqlUpdate.class)).map(SqlUpdate::value).map(resolveSql).filter(nonEmpty),
                () -> findScripts(method, resolveSql));
    }

    private static Optional<String> findScripts(Method method, Function<String, String> resolveSql) {
        final SqlScripts scripts = method.getAnnotation(SqlScripts.class);
        if (scripts != null) {
            return Optional.of(Arrays.stream(scripts.value()).map(s -> scriptValue(s, method)).map(resolveSql).collect(Collectors.joining(" ; ")));
        }
        final SqlScript script = method.getAnnotation(SqlScript.class);
        if (script != null) {
            return Optional.of(resolveSql.apply(scriptValue(script, method)));
        }
        return Optional.empty();
    }

    private static String scriptValue(SqlScript script, Method method) {
        return script.value().isEmpty() ? method.getName() : script.value();
    }
}
