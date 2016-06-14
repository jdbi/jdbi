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

import org.jdbi.v3.Call;
import org.jdbi.v3.Handle;
import org.jdbi.v3.OutParameters;
import org.jdbi.v3.util.GenericTypes;

class CallHandler extends CustomizingStatementHandler
{
    private final Class<?> sqlObjectType;
    private final SqlObject config;
    private final boolean returnOutParams;

    CallHandler(Class<?> sqlObjectType, Method method, SqlObject config)
    {
        super(sqlObjectType, method);
        this.sqlObjectType = sqlObjectType;
        this.config = config;

        Type returnType = GenericTypes.resolveType(method.getGenericReturnType(), sqlObjectType);
        Class<?> returnClass = GenericTypes.getErasedType(returnType);
        if (Void.TYPE.equals(returnClass)) {
            returnOutParams = false;
        } else if (OutParameters.class.isAssignableFrom(returnClass)) {
            returnOutParams = true;
        } else {
            throw new IllegalArgumentException("@SqlCall methods may only return null or OutParameters at present");
        }
    }

    @Override
    public Object invoke(Supplier<Handle> handle, Object target, Object[] args, Method method)
    {
        String sql = config.getSqlLocator().locate(sqlObjectType, method);
        Call call = handle.get().createCall(sql);
        populateSqlObjectData(call.getContext());
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
