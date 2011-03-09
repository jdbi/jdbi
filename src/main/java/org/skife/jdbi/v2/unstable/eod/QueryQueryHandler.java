package org.skife.jdbi.v2.unstable.eod;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.Query;

import java.util.List;

public class QueryQueryHandler extends BaseQueryHandler
{
    public QueryQueryHandler(ResolvedMethod method)
    {
        super(method);
    }

    @Override
    protected Object result(Query q, HandleDing ding)
    {
        return q;
    }

    @Override
    protected ResolvedType mapTo()
    {
        // extract T from Query<T>
        ResolvedType query_type = getMethod().getReturnType();
        List<ResolvedType> query_return_types = query_type.typeParametersFor(org.skife.jdbi.v2.Query.class);
        return query_return_types.get(0);
    }
}
