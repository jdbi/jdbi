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

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

public class SqlQueryHandler extends CustomizingStatementHandler<Query> {
    private final ResultReturner magic;

    public SqlQueryHandler(Class<?> sqlObjectType, Method method) {
        super(sqlObjectType, method);
        this.magic = ResultReturner.forMethod(sqlObjectType, method);
    }

    @Override
    void configureReturner(Query q, SqlObjectStatementConfiguration cfg) {
        cfg.setReturner(() -> {
            StatementContext ctx = q.getContext();
            Type elementType = magic.elementType(ctx);
            UseRowMapper useRowMapper = getMethod().getAnnotation(UseRowMapper.class);
            ResultIterable<?> iterable = useRowMapper == null
                    ? q.mapTo(elementType)
                    : q.map(rowMapperFor(useRowMapper));
            return magic.result(iterable, ctx);
        });
    }

    @Override
    Query createStatement(Handle handle, String locatedSql) {
        return handle.createQuery(locatedSql);
    }
}
