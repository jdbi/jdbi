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
package org.jdbi.v3.sqlobject.statement.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.extension.ExtensionMethod;
import org.jdbi.v3.core.statement.SqlStatement;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.statement.SqlPreflight;
import org.jdbi.v3.sqlobject.statement.SqlPreflights;

/**
 * Implements {@link SqlPreflight}. The preflight statements are run as a customizer on the SQL Object
 * method's <em>main</em> statement, during its setup phase, rather than as a handler-level decorator.
 * Running as a statement customizer means the preflight always executes on the same {@code Handle} and
 * inside the same transaction as the main statement, without depending on the (undefined) order in
 * which method decorators such as {@code @Transaction} are applied.
 *
 * <p>The method's arguments (captured on {@code SqlObjectStatementConfiguration} before customizers
 * run) are bound to each preflight statement via {@link ParameterBinder}.
 */
public class SqlPreflightFactory implements SqlStatementCustomizerFactory {

    @Override
    public SqlStatementCustomizer createForType(Annotation annotation, Class<?> sqlObjectType) {
        final List<String> sqls = sqlsOf(annotation);
        // A type-level customizer is applied to every method, so the binder cannot be built here;
        // resolve it per statement from the method the statement is actually running for.
        return stmt -> runPreflights(stmt, sqls, SqlPreflightFactory::binderFor);
    }

    @Override
    public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
        final List<String> sqls = sqlsOf(annotation);
        // The method is known here, so build the binder once and reuse it for every invocation.
        final ParameterBinder binder = new ParameterBinder(sqlObjectType, method);
        return stmt -> runPreflights(stmt, sqls, s -> binder);
    }

    private static List<String> sqlsOf(Annotation annotation) {
        final Stream<SqlPreflight> preflights = annotation instanceof SqlPreflights
                ? Stream.of(((SqlPreflights) annotation).value())
                : Stream.of((SqlPreflight) annotation);
        return preflights.map(SqlPreflight::value).toList();
    }

    private static ParameterBinder binderFor(SqlStatement<?> mainStatement) {
        final ExtensionMethod extensionMethod = mainStatement.getContext().getExtensionMethod();
        return new ParameterBinder(extensionMethod.getType(), extensionMethod.getMethod());
    }

    private static void runPreflights(SqlStatement<?> mainStatement, List<String> sqls,
            Function<SqlStatement<?>, ParameterBinder> binderSupplier) throws SQLException {
        final Handle handle = mainStatement.getHandle();
        final ParameterBinder binder = binderSupplier.apply(mainStatement);
        final Object[] args = mainStatement.getConfig(SqlObjectStatementConfiguration.class).getArgs();

        for (String sql : sqls) {
            try (Update preflight = handle.createUpdate(sql)) {
                // The method's arguments are bound to every preflight statement; a preflight that
                // references only some (or none) of them must not fail the unused-binding check.
                preflight.getConfig(SqlStatements.class).setUnusedBindingAllowed(true);
                binder.apply(preflight, args);
                preflight.execute();
            }
        }
    }
}
