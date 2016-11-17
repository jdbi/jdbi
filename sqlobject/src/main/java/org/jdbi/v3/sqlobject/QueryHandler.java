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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleSupplier;
import org.jdbi.v3.core.Query;

class QueryHandler extends CustomizingStatementHandler
{
    private final Class<?> sqlObjectType;
    private final ResultReturnThing magic;

    QueryHandler(Class<?> sqlObjectType, Method method, ResultReturnThing magic)
    {
        super(sqlObjectType, method);
        this.sqlObjectType = sqlObjectType;
        this.magic = magic;
    }

    @Override
    public Object invoke(Object target, Method method, Object[] args, HandleSupplier handle)
    {
        Handle h = handle.getHandle();
        String sql = h.getConfig(SqlObjects.class).getSqlLocator().locate(sqlObjectType, method);
        Query<?> q = h.createQuery(sql);
        applyCustomizers(q, args);
        applyBinders(q, args);

        return magic.map(method, q, handle);
    }
}
