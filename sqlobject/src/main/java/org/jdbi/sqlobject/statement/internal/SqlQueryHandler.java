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

import org.jdbi.core.Handle;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.core.result.ResultIterable;
import org.jdbi.core.statement.Query;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.sqlobject.statement.UseRowMapper;
import org.jdbi.sqlobject.statement.UseRowReducer;

public class SqlQueryHandler extends CustomizingStatementHandler<Query> {
    private final ResultReturner resultReturner;
    private final UseRowMapper useRowMapper;
    private final UseRowReducer useRowReducer;

    public SqlQueryHandler(Class<?> sqlObjectType, Method method) {
        super(sqlObjectType, method);
        this.resultReturner = ResultReturner.forMethod(sqlObjectType, method);

        this.useRowMapper = method.getAnnotation(UseRowMapper.class);
        this.useRowReducer = method.getAnnotation(UseRowReducer.class);

        if (this.useRowReducer != null && this.useRowMapper != null) {
            throw new IllegalStateException("Cannot declare @UseRowMapper and @UseRowReducer on the same method.");
        }
    }

    @Override
    protected void warm(ConfigRegistry config) {
        resultReturner.warm(config);
    }

    @Override
    void configureReturner(Query query, SqlObjectStatementConfiguration cfg) {

        cfg.setReturner(() -> {
            StatementContext ctx = query.getContext();
            QualifiedType<?> elementType = resultReturner.elementType(ctx.getConfig());

            if (useRowReducer != null) {
                return resultReturner.reducedResult(query.reduceRows(rowReducerFor(useRowReducer)), ctx);
            }

            ResultIterable<?> iterable = useRowMapper == null
                    ? query.mapTo(elementType)
                    : query.map(rowMapperFor(useRowMapper));
            return resultReturner.mappedResult(iterable, ctx);
        });
    }

    @Override
    Query createStatement(Handle handle, String locatedSql) {
        return handle.createQuery(locatedSql);
    }
}
