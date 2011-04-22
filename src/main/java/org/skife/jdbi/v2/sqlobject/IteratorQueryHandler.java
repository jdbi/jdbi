package org.skife.jdbi.v2.sqlobject;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.ResultIterator;

import java.util.Iterator;
import java.util.List;

class IteratorQueryHandler extends BaseQueryHandler
{
    public IteratorQueryHandler(Class sqlObjectType, ResolvedMethod method)
    {
        super(sqlObjectType, method);
    }

    @Override
    protected Object result(Query q, final HandleDing baton)
    {
        baton.retain("iterator");
        final ResultIterator itty = q.iterator();

        return new ResultIterator()
        {
            public void close()
            {
                itty.close();
            }

            public boolean hasNext()
            {
                boolean has_next = itty.hasNext();
                if (!has_next) {
                    baton.release("iterator");
                }
                return itty.hasNext();
            }

            public Object next()
            {
                Object rs = itty.next();
                boolean has_next = itty.hasNext();
                if (!has_next) {
                    baton.release("iterator");
                }
                return rs;
            }

            public void remove()
            {
                itty.remove();
            }
        };
    }

    @Override
    protected ResolvedType mapTo()
    {
        // extract T from Iterator<T>
        ResolvedType query_type = getMethod().getReturnType();
        List<ResolvedType> query_return_types = query_type.typeParametersFor(Iterator.class);
        return query_return_types.get(0);
    }
}
