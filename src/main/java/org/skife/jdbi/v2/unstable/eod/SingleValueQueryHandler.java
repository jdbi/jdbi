package org.skife.jdbi.v2.unstable.eod;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.Query;

class SingleValueQueryHandler extends BaseQueryHandler
{
    public SingleValueQueryHandler(ResolvedMethod method)
    {
        super(method);
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
