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
import org.jdbi.core.internal.MemoizingSupplier;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.core.result.ResultBearing;
import org.jdbi.core.result.ResultIterable;
import org.jdbi.core.statement.Customizable;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.core.statement.StatementTemplate;
import org.jdbi.sqlobject.statement.UseRowMapper;
import org.jdbi.sqlobject.statement.UseRowReducer;

public class SqlQueryHandler extends CustomizingStatementHandler {
    private final ResultReturner resultReturner;
    private final UseRowMapper useRowMapper;
    private final UseRowReducer useRowReducer;
    private final boolean late;

    public SqlQueryHandler(Class<?> sqlObjectType, Method method) {
        super(sqlObjectType, method);
        this.resultReturner = ResultReturner.forMethod(sqlObjectType, method);

        this.useRowMapper = method.getAnnotation(UseRowMapper.class);
        this.useRowReducer = method.getAnnotation(UseRowReducer.class);

        if (this.useRowReducer != null && this.useRowMapper != null) {
            throw new IllegalStateException("Cannot declare @UseRowMapper and @UseRowReducer on the same method.");
        }

        // A method whose customizer must mutate configuration per invocation runs on the classic path.
        this.late = hasLateCustomizers();
    }

    @Override
    protected void warm(ConfigRegistry config) {
        resultReturner.warm(config);
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
    void configureReturner(Customizable<?> stmt, SqlObjectStatementState state) {
        final ResultBearing results = (ResultBearing) stmt;
        state.setReturner(() -> {
            StatementContext ctx = stmt.getContext();
            QualifiedType<?> elementType = resultReturner.elementType(ctx.getConfig());

            if (useRowReducer != null) {
                return resultReturner.reducedResult(results.reduceRows(rowReducerFor(useRowReducer)), ctx);
            }

            ResultIterable<?> iterable = useRowMapper == null
                    ? results.mapTo(elementType)
                    : results.map(rowMapperFor(useRowMapper));
            return resultReturner.mappedResult(iterable, ctx);
        });
    }

    @Override
    Customizable<?> createStatement(Handle handle, String locatedSql) {
        return handle.createQuery(locatedSql);
    }
}
