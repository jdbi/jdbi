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
package org.skife.jdbi.v2.sqlobject;

import com.fasterxml.classmate.members.ResolvedMethod;
import net.sf.cglib.proxy.MethodProxy;
import org.skife.jdbi.v2.Call;
import org.skife.jdbi.v2.ConcreteStatementContext;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.OutParameters;

class CallHandler extends CustomizingStatementHandler
{
    private final String sql;
    private final boolean returnOutParams;

    CallHandler(Class<?> sqlObjectType, ResolvedMethod method, ParameterBinderRegistry binderRegistry)
    {
        super(sqlObjectType, method, binderRegistry);

        if (null != method.getReturnType() ) {
            if (method.getReturnType().isInstanceOf(OutParameters.class)){
                returnOutParams = true;
            }
            else {
                throw new IllegalArgumentException("@SqlCall methods may only return null or OutParameters at present");
            }
        }
        else {
            returnOutParams = false;
        }

        this.sql = SqlObject.getSql(method.getRawMember().getAnnotation(SqlCall.class), method.getRawMember());
    }

    @Override
    public Object invoke(HandleDing ding, Object target, Object[] args, MethodProxy mp)
    {
        Handle h = ding.getHandle();
        Call call = h.createCall(sql);
        populateSqlObjectData((ConcreteStatementContext)call.getContext());
        applyCustomizers(call, args);
        applyBinders(call, args);

        OutParameters ou = call.invoke();

        if (returnOutParams) {
            return ou;
        }
        else {
            return null;
        }
    }
}
