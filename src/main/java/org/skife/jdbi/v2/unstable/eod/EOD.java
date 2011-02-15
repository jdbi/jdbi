package org.skife.jdbi.v2.unstable.eod;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * All methods will be moved onto DBI or Handle when this moves out of unstable.
 */
public class EOD
{
    private static final TypeResolver tr = new TypeResolver();

    private static final Map<DBI, Mapamajig> attributes = Collections
        .synchronizedMap(new WeakHashMap<DBI, Mapamajig>());

    public static void addMapper(DBI dbi, ResultSetMapper mapper)
    {
        mapamaget(dbi).add(mapper);
    }

    private static Mapamajig mapamaget(DBI dbi)
    {
        if (attributes.containsKey(dbi)) {
            return attributes.get(dbi);
        }
        else {
            synchronized (attributes) {
                if (attributes.containsKey(dbi)) {
                    return attributes.get(dbi);
                }
                else {
                    Mapamajig m = new Mapamajig();
                    attributes.put(dbi, m);
                    return m;
                }
            }
        }
    }


    public static <T extends CloseMe> T open(DBI dbi, Class<T> sqlObjectType)
    {
        final ResolvedType sql_object_type = tr.resolve(sqlObjectType);

        MemberResolver mr = new MemberResolver(tr);
        ResolvedTypeWithMembers d = mr.resolve(sql_object_type, null, null);
        final Map<Method, Handler> handlers = new HashMap<Method, Handler>();
        for (final ResolvedMethod method : d.getMemberMethods()) {
            if (method.getRawMember().isAnnotationPresent(Sql.class)) {
                if (method.getReturnType().isInstanceOf(Query.class)) {
                    handlers.put(method.getRawMember(), new QueryHandler(method, dbi));
                }
                else if (method.getReturnType().isInstanceOf(List.class)) {
                    handlers.put(method.getRawMember(), new ListHandler(method, dbi));
                }
                else if (method.getReturnType().isInstanceOf(Iterator.class)) {
                    handlers.put(method.getRawMember(), new IteratorHandler(method, dbi));
                }
                else {
                    handlers.put(method.getRawMember(), new SingleValueHandler(method, dbi));
                }
            }
            else if (method.getName().equals("close") && method.getRawMember().getParameterTypes().length == 0) {
                handlers.put(method.getRawMember(), new CloseHandler());
            }
        }

        final Handle handle = dbi.open();
        final InvocationHandler handler = new InvocationHandler()
        {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
                return handlers.get(method).invoke(handle, args);
            }
        };

        return (T) Proxy.newProxyInstance(sqlObjectType.getClassLoader(), new Class[]{sqlObjectType}, handler);
    }


    private static class SingleValueHandler extends BaseHandler
    {
        public SingleValueHandler(ResolvedMethod method, DBI dbi)
        {
            super(method, EOD.mapamaget(dbi));
        }

        @Override
        protected Object resultType(Query q)
        {
            return q.first();
        }

        @Override
        protected ResolvedType mapTo()
        {
            return getMethod().getReturnType();
        }
    }

    private static class IteratorHandler extends BaseHandler
    {
        public IteratorHandler(ResolvedMethod method, DBI dbi)
        {
            super(method, EOD.mapamaget(dbi));
        }

        @Override
        protected Object resultType(Query q)
        {
            return q.iterator();
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

    private static class ListHandler extends BaseHandler
    {
        public ListHandler(ResolvedMethod method, DBI dbi)
        {
            super(method, EOD.mapamaget(dbi));
        }

        @Override
        protected Object resultType(Query q)
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

    private static class QueryHandler extends BaseHandler
    {
        public QueryHandler(ResolvedMethod method, DBI dbi)
        {
            super(method, EOD.mapamaget(dbi));
        }

        @Override
        protected Object resultType(Query q)
        {
            return q;
        }

        @Override
        protected ResolvedType mapTo()
        {
            // extract T from Query<T>
            ResolvedType query_type = getMethod().getReturnType();
            List<ResolvedType> query_return_types = query_type.typeParametersFor(Query.class);
            return query_return_types.get(0);
        }
    }
}
