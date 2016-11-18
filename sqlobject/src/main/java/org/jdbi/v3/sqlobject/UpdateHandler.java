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
package org.jdbi.v3.sqlobject;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.jdbi.v3.core.GeneratedKeys;
import org.jdbi.v3.core.HandleSupplier;
import org.jdbi.v3.core.Update;
import org.jdbi.v3.core.exception.UnableToCreateStatementException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.util.GenericTypes;
import org.jdbi.v3.sqlobject.exceptions.UnableToCreateSqlObjectException;

class UpdateHandler extends CustomizingStatementHandler
{
    private final Class<?> sqlObjectType;
    private final Returner returner;

    UpdateHandler(Class<?> sqlObjectType, Method method)
    {
        super(sqlObjectType, method);
        this.sqlObjectType = sqlObjectType;

        boolean isGetGeneratedKeys = method.isAnnotationPresent(GetGeneratedKeys.class);

        Type returnType = GenericTypes.resolveType(method.getGenericReturnType(), sqlObjectType);
        if (isGetGeneratedKeys) {
            final ResultReturnThing magic = ResultReturnThing.forMethod(sqlObjectType, method);
            final GetGeneratedKeys ggk = method.getAnnotation(GetGeneratedKeys.class);
            final RowMapper<?> mapper;
            if (DefaultGeneratedKeyMapper.class.equals(ggk.value())) {
                mapper = new DefaultGeneratedKeyMapper(returnType, ggk.columnName());
            }
            else {
                try {
                    mapper = ggk.value().newInstance();
                }
                catch (Exception e) {
                    throw new UnableToCreateStatementException("Unable to instantiate row mapper for statement", e, null);
                }
            }
            this.returner = (update, handle) -> {
                GeneratedKeys<?> o = update.executeAndReturnGeneratedKeys(mapper, ggk.columnName());
                return magic.result(o, handle);
            };
        } else if (isNumeric(method.getReturnType())) {
            this.returner = (update, handle) -> update.execute();
        } else if (isBoolean(method.getReturnType())) {
            this.returner = (update, handle) -> update.execute() > 0;
        } else {
            throw new UnableToCreateSqlObjectException(invalidReturnTypeMessage(method, returnType));
        }
    }

    @Override
    public Object invoke(Object target, Method method, Object[] args, HandleSupplier handle)
    {
        String sql = handle.getConfig(SqlObjects.class).getSqlLocator().locate(sqlObjectType, method);
        Update q = handle.getHandle().createUpdate(sql);
        applyCustomizers(q, args);
        applyBinders(q, args);
        return this.returner.value(q, handle);
    }


    private interface Returner
    {
        Object value(Update update, HandleSupplier handle);
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
