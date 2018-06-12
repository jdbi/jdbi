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
import java.util.function.Function;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.result.ResultBearing;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.sqlobject.UnableToCreateSqlObjectException;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;

public class SqlUpdateHandler extends AbstractCustomizingStatementHandler<Update> {
    private final Function<Update, Object> returner;

    public SqlUpdateHandler(Class<?> sqlObjectType, Method method) {
        super(sqlObjectType, method);

        if (method.isAnnotationPresent(UseRowReducer.class)) {
            throw new UnsupportedOperationException("Cannot declare @UseRowReducer on a @SqlUpdate method.");
        }

        boolean isGetGeneratedKeys = method.isAnnotationPresent(GetGeneratedKeys.class);

        Type returnType = GenericTypes.resolveType(method.getGenericReturnType(), sqlObjectType);
        if (isGetGeneratedKeys) {
            AbstractResultReturner magic = AbstractResultReturner.forMethod(sqlObjectType, method);

            String[] columnNames = method.getAnnotation(GetGeneratedKeys.class).value();

            this.returner = update -> {
                ResultBearing resultBearing = update.executeAndReturnGeneratedKeys(columnNames);

                UseRowMapper useRowMapper = method.getAnnotation(UseRowMapper.class);
                ResultIterable<?> iterable = useRowMapper == null
                        ? resultBearing.mapTo(returnType)
                        : resultBearing.map(rowMapperFor(useRowMapper));

                return magic.mappedResult(iterable, update.getContext());
            };
        } else if (isNumeric(method.getReturnType())) {
            this.returner = update -> update.execute();
        } else if (isBoolean(method.getReturnType())) {
            this.returner = update -> update.execute() > 0;
        } else {
            throw new UnableToCreateSqlObjectException(invalidReturnTypeMessage(method, returnType));
        }
    }

    @Override
    Update createStatement(Handle handle, String locatedSql) {
        return handle.createUpdate(locatedSql);
    }

    @Override
    void configureReturner(Update u, SqlObjectStatementConfiguration cfg) {
        cfg.setReturner(() -> returner.apply(u));
    }

    private boolean isNumeric(Class<?> type) {
        return Number.class.isAssignableFrom(type)
            || type.equals(int.class)
            || type.equals(long.class)
            || type.equals(void.class);
    }

    private boolean isBoolean(Class<?> type) {
        return type.equals(boolean.class) || type.equals(Boolean.class);
    }

    private String invalidReturnTypeMessage(Method method, Type returnType) {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName()
                + " method is annotated with @SqlUpdate so should return void, boolean, or Number but is returning: "
                + returnType;
    }
}
