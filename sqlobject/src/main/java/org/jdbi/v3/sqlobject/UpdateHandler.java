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
import java.util.function.Supplier;

import org.jdbi.v3.ConcreteStatementContext;
import org.jdbi.v3.GeneratedKeys;
import org.jdbi.v3.Handle;
import org.jdbi.v3.Types;
import org.jdbi.v3.Update;
import org.jdbi.v3.exceptions.UnableToCreateStatementException;
import org.jdbi.v3.sqlobject.exceptions.UnableToCreateSqlObjectException;
import org.jdbi.v3.tweak.RowMapper;

class UpdateHandler extends CustomizingStatementHandler
{
    private final String sql;
    private final Returner returner;

    UpdateHandler(Class<?> sqlObjectType, Method method)
    {
        super(sqlObjectType, method);

        Type returnType = Types.resolveType(method.getGenericReturnType(), sqlObjectType);
        if(returnTypeIsInvalid(method.getReturnType()) ) {
            throw new UnableToCreateSqlObjectException(invalidReturnTypeMessage(method, returnType));
        }
        this.sql = SqlAnnotations.getSql(method.getAnnotation(SqlUpdate.class), method);
        if (method.isAnnotationPresent(GetGeneratedKeys.class)) {

            final ResultReturnThing magic = ResultReturnThing.forMethod(sqlObjectType, method);
            final GetGeneratedKeys ggk = method.getAnnotation(GetGeneratedKeys.class);
            final RowMapper<?> mapper;
            try {
                mapper = ggk.value().newInstance();
            }
            catch (Exception e) {
                throw new UnableToCreateStatementException("Unable to instantiate row mapper for statement", e, null);
            }
            this.returner = (update, handle) -> {
                GeneratedKeys<?> o = update.executeAndReturnGeneratedKeys(mapper, ggk.columnName());
                return magic.result(o, handle);
            };
        }
        else {
            this.returner = (update, handle) -> update.execute();
        }
    }

    @Override
    public Object invoke(Supplier<Handle> handle, Object target, Object[] args, Method method)
    {
        Update q = handle.get().createStatement(sql);
        populateSqlObjectData((ConcreteStatementContext)q.getContext());
        applyCustomizers(q, args);
        applyBinders(q, args);
        return this.returner.value(q, handle);
    }


    private interface Returner
    {
        Object value(Update update, Supplier<Handle> handle);
    }

    private boolean returnTypeIsInvalid(Class<?> type) {
        return !Number.class.isAssignableFrom(type) &&
                !type.equals(Integer.TYPE) &&
                !type.equals(Long.TYPE) &&
                !type.equals(Void.TYPE);
    }

    private String invalidReturnTypeMessage(Method method, Type returnType) {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName() +
                " method is annotated with @SqlUpdate so should return void or Number but is returning: " +
                returnType;
    }
}
