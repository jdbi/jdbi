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
import java.util.function.Function;

import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.result.ResultSetIterable;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.sqlobject.Handler;
import org.jdbi.v3.sqlobject.HandlerFactory;
import org.jdbi.v3.sqlobject.SqlMethodAnnotation;
import org.jdbi.v3.sqlobject.SqlObjects;
import org.jdbi.v3.sqlobject.UnableToCreateSqlObjectException;

/**
 * Used to indicate that a method should execute a non-query sql statement
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@SqlMethodAnnotation(SqlUpdate.Factory.class)
public @interface SqlUpdate {
    /**
     * The sql (or statement name if using a statement locator) to be executed. The default value will use
     * the method name of the method being annotated. This default behavior is only useful in conjunction
     * with a statement locator.
     *
     * @return the SQL string (or name)
     */
    String value() default "";

    class Factory implements HandlerFactory {
        @Override
        public Handler buildHandler(Class<?> sqlObjectType, Method method) {
            return new UpdateHandler(sqlObjectType, method);
        }
    }

    class UpdateHandler extends CustomizingStatementHandler {
        private final Class<?> sqlObjectType;
        private final Function<Update, Object> returner;

        UpdateHandler(Class<?> sqlObjectType, Method method) {
            super(sqlObjectType, method);
            this.sqlObjectType = sqlObjectType;

            boolean isGetGeneratedKeys = method.isAnnotationPresent(GetGeneratedKeys.class);

            Type returnType = GenericTypes.resolveType(method.getGenericReturnType(), sqlObjectType);
            if (isGetGeneratedKeys) {
                final ResultReturner magic = ResultReturner.forMethod(sqlObjectType, method);
                final GetGeneratedKeys ggk = method.getAnnotation(GetGeneratedKeys.class);
                final RowMapper<?> mapper = ResultReturner.rowMapperFor(ggk, returnType);

                this.returner = update -> {
                    String columnName = ggk.columnName();
                    ResultSetIterable resultSetIterable = columnName.isEmpty()
                            ? update.executeAndReturnGeneratedKeys()
                            : update.executeAndReturnGeneratedKeys(columnName);
                    return magic.result(resultSetIterable.map(mapper), update.getContext());
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
        public Object invoke(Object target, Object[] args, HandleSupplier handle) {
            String sql = handle.getConfig(SqlObjects.class).getSqlLocator().locate(sqlObjectType, getMethod());
            Update update = handle.getHandle().createUpdate(sql);
            applyCustomizers(update, args);
            return this.returner.apply(update);
        }

        private boolean isNumeric(Class<?> type) {
            return Number.class.isAssignableFrom(type) ||
                    type.equals(int.class) ||
                    type.equals(long.class) ||
                    type.equals(void.class);
        }

        private boolean isBoolean(Class<?> type) {
            return type.equals(boolean.class) ||
                    type.equals(Boolean.class);
        }

        private String invalidReturnTypeMessage(Method method, Type returnType) {
            return method.getDeclaringClass().getSimpleName() + "." + method.getName() +
                    " method is annotated with @SqlUpdate so should return void, boolean, or Number but is returning: " +
                    returnType;
        }
    }
}
