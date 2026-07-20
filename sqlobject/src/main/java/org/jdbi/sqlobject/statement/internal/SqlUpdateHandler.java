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
package org.jdbi.sqlobject.statement.internal;

import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jdbi.core.Handle;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.generic.GenericTypes;
import org.jdbi.core.internal.MemoizingSupplier;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.core.qualifier.Qualifiers;
import org.jdbi.core.result.ResultBearing;
import org.jdbi.core.result.ResultIterable;
import org.jdbi.core.statement.Customizable;
import org.jdbi.core.statement.Query;
import org.jdbi.core.statement.StatementTemplate;
import org.jdbi.sqlobject.UnableToCreateSqlObjectException;
import org.jdbi.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.sqlobject.statement.UseRowMapper;
import org.jdbi.sqlobject.statement.UseRowReducer;

import static java.lang.String.format;

public class SqlUpdateHandler extends CustomizingStatementHandler {

    private final WarmableResultTransformer resultTransformer;
    private final boolean late;

    public SqlUpdateHandler(Class<?> sqlObjectType, Method method) {
        super(sqlObjectType, method);

        if (method.isAnnotationPresent(UseRowReducer.class)) {
            throw new UnsupportedOperationException("Cannot declare @UseRowReducer on a @SqlUpdate method.");
        }

        GetGeneratedKeys getGeneratedKeys = method.getAnnotation(GetGeneratedKeys.class);

        QualifiedType<?> returnType = QualifiedType.of(
                        GenericTypes.resolveType(method.getGenericReturnType(), sqlObjectType))
                .withAnnotations(new Qualifiers().findFor(method));

        if (getGeneratedKeys != null) {
            String[] columnNames = getGeneratedKeys.value();
            var resultReturner = ResultReturner.forMethod(sqlObjectType, method);

            this.resultTransformer = new WarmableResultTransformer() {
                @Override
                public Object apply(Query query) {
                    var ctx = query.getContext();
                    var elementType = resultReturner.elementType(ctx.getConfig());
                    ResultBearing resultBearing = query.executeAndReturnGeneratedKeys(columnNames);

                    UseRowMapper useRowMapper = method.getAnnotation(UseRowMapper.class);
                    ResultIterable<?> iterable = useRowMapper == null
                        ? resultBearing.mapTo(elementType)
                        : resultBearing.map(rowMapperFor(useRowMapper));

                    return resultReturner.mappedResult(iterable, query.getContext());
                }

                @Override
                public void warm(ConfigRegistry config) {
                    resultReturner.warm(config);
                }
            };
        } else if (isLong(method.getReturnType())) {
            this.resultTransformer = Query::executeLarge;
        } else if (isNumeric(method.getReturnType())) {
            this.resultTransformer = Query::execute;
        } else if (isBoolean(method.getReturnType())) {
            this.resultTransformer = query -> query.execute() > 0;
        } else {
            throw new UnableToCreateSqlObjectException(format(
                    "%s.%s method is annotated with @SqlUpdate and should return void, boolean, int, long, or have a @GetGeneratedKeys annotation, but is returning: %s",
                    method.getDeclaringClass().getSimpleName(), method.getName(), returnType));
        }

        // A method whose customizer must mutate configuration per invocation runs on the classic path.
        this.late = hasLateCustomizers();
    }

    @Override
    protected void warm(ConfigRegistry config) {
        this.resultTransformer.warm(config);
    }

    @Override
    Function<Handle, ? extends Customizable<?>> statementFactory(ConfigRegistry config, Supplier<String> locatedSql) {
        if (late) {
            // Classic path: a fresh Query per call, each with its own configuration copy.
            return super.statementFactory(config, locatedSql);
        }
        // Fast path: build one template per attach, baking configure-phase customizers into its
        // configuration snapshot once. Every call binds against a fresh, thread-confined binding.
        final Supplier<StatementTemplate> template = MemoizingSupplier.of(() -> {
            final ConfigRegistry templateConfig = config.createCopy();
            applyConfigureCustomizers(new ConfigureStatement(templateConfig));
            return new StatementTemplate(templateConfig, locatedSql.get());
        });
        return handle -> template.get().with(handle);
    }

    @Override
    void applyPerInvocationCustomizers(Customizable<?> stmt, Object[] args) {
        if (late) {
            super.applyPerInvocationCustomizers(stmt, args);
        } else {
            // Configure-phase customizers are baked into the template; only bind per invocation.
            applyCustomizers(stmt, args, Phase.BIND);
        }
    }

    @Override
    Query createStatement(Handle handle, String locatedSql) {
        return handle.createQuery(locatedSql);
    }

    @Override
    void configureReturner(Customizable<?> stmt, SqlObjectStatementState state) {
        final Query query = (Query) stmt;
        state.setReturner(() -> resultTransformer.apply(query));
    }

    private boolean isNumeric(Class<?> type) {
        return type.equals(Integer.class)
                || type.equals(int.class)
                || type.equals(void.class)
                || type.equals(Void.class);
    }

    private boolean isBoolean(Class<?> type) {
        return type.equals(boolean.class) || type.equals(Boolean.class);
    }

    private boolean isLong(Class<?> type) {
        return type.equals(long.class) || type.equals(Long.class);
    }

    @SuppressWarnings("PMD.ImplicitFunctionalInterface")
    private interface WarmableResultTransformer extends Function<Query, Object> {
        default void warm(ConfigRegistry config) {}
    }
}
