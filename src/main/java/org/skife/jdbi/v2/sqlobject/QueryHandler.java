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
import org.skife.jdbi.v2.ConcreteStatementContext;
import org.skife.jdbi.v2.Query;

class QueryHandler extends CustomizingStatementHandler
{
    private final String            sql;
    private final ResolvedMethod    method;
    private final ResultReturnThing magic;

    QueryHandler(Class<?> sqlObjectType, ResolvedMethod method, ResultReturnThing magic)
    {
        super(sqlObjectType, method);
        this.method = method;
        this.magic = magic;
        this.sql = SqlObject.getSql(method.getRawMember().getAnnotation(SqlQuery.class), method.getRawMember());
    }

    @Override
    public Object invoke(HandleDing h, Object target, Object[] args, MethodProxy mp)
    {
        Query q = h.getHandle().createQuery(sql);
        populateSqlObjectData((ConcreteStatementContext) q.getContext());
        applyCustomizers(q, args);
        applyBinders(q, args);

        return magic.map(method, q, h);
    }
}
