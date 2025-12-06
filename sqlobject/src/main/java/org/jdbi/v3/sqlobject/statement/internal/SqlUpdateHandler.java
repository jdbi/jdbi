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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.core.result.ResultBearing;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.sqlobject.UnableToCreateSqlObjectException;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;

import static java.lang.String.format;

public class SqlUpdateHandler extends CustomizingStatementHandler<Update> {

    private final UpdateReturner updateReturner;

    public SqlUpdateHandler(Class<?> sqlObjectType, Method method) {
        super(sqlObjectType, method);

        if (method.isAnnotationPresent(UseRowReducer.class)) {
            throw new UnsupportedOperationException("Cannot declare @UseRowReducer on a @SqlUpdate method.");
        }

        if (method.isAnnotationPresent(GetGeneratedKeys.class)) {
            this.updateReturner = new GetGeneratedKeysUpdateReturner(sqlObjectType, method);
        } else if (isLong(method.getReturnType())) {
            this.updateReturner = Update::executeLarge;
        } else if (isNumeric(method.getReturnType())) {
            this.updateReturner = Update::execute;
        } else if (isBoolean(method.getReturnType())) {
            this.updateReturner = update -> update.execute() > 0;
        } else {
            QualifiedType<?> returnType = QualifiedType.of(
                            GenericTypes.resolveType(method.getGenericReturnType(), sqlObjectType))
                    .withAnnotations(new Qualifiers().findFor(method));
            throw new UnableToCreateSqlObjectException(format(
                    "%s.%s method is annotated with @SqlUpdate so should return void, boolean, int, or long but is returning: %s",
                    method.getDeclaringClass().getSimpleName(), method.getName(), returnType));
        }
    }

    @Override
    protected void warm(ConfigRegistry config) {
        updateReturner.warm(config);
    }

    @Override
    Update createStatement(Handle handle, String locatedSql) {
        return handle.createUpdate(locatedSql);
    }

    @Override
    void configureReturner(Update u, SqlObjectStatementConfiguration cfg) {
        cfg.setReturner(() -> updateReturner.result(u));
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


    @FunctionalInterface
    interface UpdateReturner {

        Object result(Update update);

        default void warm(ConfigRegistry config) {}
    }

    static class GetGeneratedKeysUpdateReturner implements UpdateReturner {
        private final ResultReturner resultReturner;

        private final GetGeneratedKeys getGeneratedKeys;
        private final UseRowMapper useRowMapper;

        GetGeneratedKeysUpdateReturner(Class<?> sqlObjectType, Method method) {
            this.resultReturner = ResultReturner.forMethod(sqlObjectType, method);
            this.getGeneratedKeys = method.getAnnotation(GetGeneratedKeys.class);
            this.useRowMapper = method.getAnnotation(UseRowMapper.class);
        }

        @Override
        public Object result(Update update) {
            StatementContext ctx = update.getContext();
            QualifiedType<?> elementType = resultReturner.elementType(ctx.getConfig());

            ResultBearing resultBearing = update.executeAndReturnGeneratedKeys(getGeneratedKeys.value());

            ResultIterable<?> iterable = useRowMapper == null
                    ? resultBearing.mapTo(elementType)
                    : resultBearing.map(rowMapperFor(useRowMapper));

            return resultReturner.mappedResult(iterable, update.getContext());
        }

        @Override
        public void warm(ConfigRegistry config) {
            resultReturner.warm(config);
        }

    }
}
