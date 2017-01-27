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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.Handler;
import org.jdbi.v3.sqlobject.HandlerFactory;
import org.jdbi.v3.sqlobject.SqlMethodAnnotation;

/**
 * Used to indicate that a method should execute a query.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@SqlMethodAnnotation(SqlQuery.Factory.class)
public @interface SqlQuery {
    /**
     * The query (or query name if using a statement locator) to be executed. The default value will use
     * the method name of the method being annotated. This default behavior is only useful in conjunction
     * with a statement locator.
     *
     * @return the SQL string (or name)
     */
    String value() default "";

    class Factory implements HandlerFactory {
        @Override
        public Handler buildHandler(Class<?> sqlObjectType, Method method) {
            return new QueryHandler(sqlObjectType, method);
        }
    }

    class QueryHandler extends CustomizingStatementHandler<Query> {
        private final ResultReturner magic;

        QueryHandler(Class<?> sqlObjectType, Method method) {
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
}
