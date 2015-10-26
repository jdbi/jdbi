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

import com.fasterxml.classmate.members.ResolvedMethod;

import net.sf.cglib.proxy.MethodProxy;

import org.jdbi.v3.ConcreteStatementContext;
import org.jdbi.v3.GeneratedKeys;
import org.jdbi.v3.Update;
import org.jdbi.v3.exceptions.UnableToCreateStatementException;
import org.jdbi.v3.sqlobject.exceptions.UnableToCreateSqlObjectException;
import org.jdbi.v3.tweak.ResultSetMapper;

class UpdateHandler extends CustomizingStatementHandler
{
    private final String sql;
    private final Returner returner;

    public UpdateHandler(Class<?> sqlObjectType, ResolvedMethod method)
    {
        super(sqlObjectType, method);

        if(returnTypeIsInvalid(method.getRawMember().getReturnType()) ) {
            throw new UnableToCreateSqlObjectException(invalidReturnTypeMessage(method));
        }
        this.sql = SqlObject.getSql(method.getRawMember().getAnnotation(SqlUpdate.class), method.getRawMember());
        if (method.getRawMember().isAnnotationPresent(GetGeneratedKeys.class)) {

            final ResultReturnThing magic = ResultReturnThing.forType(method);
            final GetGeneratedKeys ggk = method.getRawMember().getAnnotation(GetGeneratedKeys.class);
            final ResultSetMapper<?> mapper;
            try {
                mapper = ggk.value().newInstance();
            }
            catch (Exception e) {
                throw new UnableToCreateStatementException("Unable to instantiate result set mapper for statement", e, null);
            }
            this.returner = (update, baton) -> {
                GeneratedKeys<?> o = update.executeAndReturnGeneratedKeys(mapper, ggk.columnName());
                return magic.result(o, baton);
            };
        }
        else {
            this.returner = (update, baton) -> update.execute();
        }
    }

    @Override
    public Object invoke(HandleDing h, Object target, Object[] args, MethodProxy mp)
    {
        Update q = h.getHandle().createStatement(sql);
        populateSqlObjectData((ConcreteStatementContext)q.getContext());
        applyCustomizers(q, args);
        applyBinders(q, args);
        return this.returner.value(q, h);
    }


    private interface Returner
    {
        Object value(Update update, HandleDing baton);
    }

    private boolean returnTypeIsInvalid(Class<?> type) {
        return !Number.class.isAssignableFrom(type) &&
                !type.equals(Integer.TYPE) &&
                !type.equals(Long.TYPE) &&
                !type.equals(Void.TYPE);
    }

    private String invalidReturnTypeMessage(ResolvedMethod method) {
        return method.getDeclaringType() + "." + method +
                " method is annotated with @SqlUpdate so should return void or Number but is returning: " +
                method.getReturnType();
    }
}
