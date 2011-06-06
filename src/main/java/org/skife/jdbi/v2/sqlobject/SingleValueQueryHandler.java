package org.skife.jdbi.v2.sqlobject;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.Query;

class SingleValueQueryHandler extends BaseQueryHandler
{
    public SingleValueQueryHandler(Class<?> sqlObjectType, ResolvedMethod method)
    {
        super(sqlObjectType, method);
    }

    @Override
    protected Object result(Query q, HandleDing baton)
    {
        return q.first();
    }

    @Override
    protected ResolvedType mapTo()
    {
        return getMethod().getReturnType();
    }
}
