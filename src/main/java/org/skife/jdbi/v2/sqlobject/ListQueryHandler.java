package org.skife.jdbi.v2.sqlobject;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.Query;

import java.util.List;

class ListQueryHandler extends BaseQueryHandler
{
    public ListQueryHandler(Class sqlObjectType, ResolvedMethod method)
    {
        super(sqlObjectType, method);
    }

    @Override
    protected Object result(Query q, HandleDing ding)
    {
        return q.list();
    }

    @Override
    protected ResolvedType mapTo()
    {
        // extract T from List<T>
        ResolvedType query_type = getMethod().getReturnType();
        List<ResolvedType> query_return_types = query_type.typeParametersFor(List.class);
        return query_return_types.get(0);

    }
}
