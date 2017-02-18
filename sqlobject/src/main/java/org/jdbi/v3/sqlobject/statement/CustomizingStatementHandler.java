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

import static java.util.stream.Stream.concat;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.SqlStatement;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jdbi.v3.sqlobject.Handler;
import org.jdbi.v3.sqlobject.SqlObjects;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer;

abstract class CustomizingStatementHandler<StatementType extends SqlStatement<StatementType>> implements Handler
{
    private final List<BoundCustomizer> statementCustomizers;
    private final Class<?> sqlObjectType;
    private final Method method;

    CustomizingStatementHandler(Class<?> type, Method method)
    {
        this.sqlObjectType = type;
        this.method = method;

        // Prepare customizers that don't depend on actual arguments.
        final Stream<BoundCustomizer> methodCustomizers = concat(
                annotationsFor(type).map(a -> instantiateFactory(a).createForType(a, type)),
                annotationsFor(method).map(a -> instantiateFactory(a).createForMethod(a, type, method)))
            .map(BoundCustomizer::of);

        // Append customizers that do.
        statementCustomizers = concat(methodCustomizers, parameterCustomizers(type, method))
            .collect(Collectors.<BoundCustomizer>toList());
    }

    private static Stream<Annotation> annotationsFor(AnnotatedElement... elements) {
        return Stream.of(elements)
                .map(AnnotatedElement::getAnnotations)
                .flatMap(Stream::of)
                .filter(a -> a.annotationType().isAnnotationPresent(SqlStatementCustomizingAnnotation.class));
    }

    private static Stream<BoundCustomizer> parameterCustomizers(Class<?> type, Method method) {
        final Parameter[] parameters = method.getParameters();

        return IntStream.range(0, parameters.length)
                .mapToObj(Integer::valueOf)
                .flatMap(i -> eachParameterCustomizers(type, method, parameters[i], i));
    }

    private static Stream<BoundCustomizer> eachParameterCustomizers(Class<?> type, Method method, Parameter parameter, Integer i) {

        List<BoundCustomizer> customizers = annotationsFor(parameter)
                .map(a -> instantiateFactory(a).createForParameter(a, type, method, parameter, i))
                .<BoundCustomizer>map(c -> (stmt, args) -> c.apply(stmt, args[i])).collect(Collectors.toList());

        if (!customizers.isEmpty()) {
            return customizers.stream();
        }

        return Stream.of(defaultParameterCustomizer(type, method, parameter, i));
    }

    /**
     * Default parameter customizer for parameters with no annotations.
     */
    private static BoundCustomizer defaultParameterCustomizer(Class<?> type, Method method, Parameter parameter, Integer i) {
        return (stmt, args) -> getDefaultParameterCustomizerFactory(stmt)
                .createForParameter(type, method, parameter, i)
                .apply(stmt, args[i]);
    }

    private static DefaultParameterCustomizerFactory getDefaultParameterCustomizerFactory(SqlStatement<?> stmt) {
        return stmt.getConfig(SqlObjects.class).getDefaultParameterCustomizerFactory();
    }

    private static SqlStatementCustomizerFactory instantiateFactory(Annotation annotation) {
        SqlStatementCustomizingAnnotation sca = annotation.annotationType()
                .getAnnotation(SqlStatementCustomizingAnnotation.class);
        try {
            return sca.value().getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("unable to instantiate sql statement customizer factory", e);
        }
    }

    @Override
    public Object invoke(Object target, Object[] args, HandleSupplier hs) throws Exception {
        final Handle h = hs.getHandle();
        final String locatedSql = locateSql(h);
        final StatementType stmt = createStatement(h, locatedSql);
        final SqlObjectStatementConfiguration cfg = stmt.getConfig(SqlObjectStatementConfiguration.class);
        configureReturner(stmt, cfg);
        applyCustomizers(stmt, args);
        return cfg.getReturner().get();
    }

    void applyCustomizers(final StatementType stmt, Object[] args) {
        statementCustomizers.forEach(b -> {
            try {
                b.apply(stmt, args);
            } catch (SQLException e) {
                throw new UnableToExecuteStatementException(e, stmt.getContext());
            }
        });
    }

    abstract void configureReturner(StatementType stmt, SqlObjectStatementConfiguration cfg);
    abstract StatementType createStatement(Handle handle, String locatedSql);

    String locateSql(final Handle h)
    {
        return h.getConfig(SqlObjects.class).getSqlLocator().locate(sqlObjectType, method);
    }

    Method getMethod()
    {
        return method;
    }

    static RowMapper<?> rowMapperFor(UseRowMapper annotation)
    {
        Class<? extends RowMapper<?>> mapperClass = annotation.value();
        try {
            return mapperClass.getConstructor().newInstance();
        }
        catch (Exception e) {
            throw new UnableToCreateStatementException("Could not create mapper " + mapperClass.getName(), e, null);
        }
    }

    /**
     * A {@link SqlStatementCustomizer} or {@link SqlStatementParameterCustomizer} that
     * is ready to apply.
     */
    private interface BoundCustomizer {
        void apply(SqlStatement<?> stmt, Object[] args) throws SQLException;

        static BoundCustomizer of(SqlStatementCustomizer c) {
            return (stmt, args) -> c.apply(stmt);
        }
    }
}
