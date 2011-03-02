package org.skife.jdbi.v2.unstable.eod;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Update;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class SqlObject implements InvocationHandler
{

    private static final TypeResolver         tr            = new TypeResolver();
    private static final Map<Method, Handler> mixinHandlers = new HashMap<Method, Handler>();

    static {
        mixinHandlers.putAll(Transactional.Helper.handlers());
        mixinHandlers.putAll(GetHandle.Helper.handlers());
    }

    private final HandleFactory handles;

    SqlObject(Class iface, HandleFactory handles)
    {
        this.handles = handles;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        return null;
    }


    static class HandleFactory
    {

    }

    private static <T> T buildSqlObject(final Class<T> sqlObjectType,
                                        final Map<Method, Handler> handlers,
                                        final Handle handle)
    {
        final InvocationHandler handler = new InvocationHandler()
        {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
                return handlers.get(method).invoke(handle, proxy, args);
            }
        };

        return (T) Proxy.newProxyInstance(sqlObjectType.getClassLoader(), new Class[]{sqlObjectType}, handler);
    }

    private static Map<Method, Handler> buildHandlersFor(Class sqlObjectType) throws NoSuchMethodException
    {
        final MemberResolver mr = new MemberResolver(tr);
        final ResolvedType sql_object_type = tr.resolve(sqlObjectType);

        final ResolvedTypeWithMembers d = mr.resolve(sql_object_type, null, null);

        final Map<Method, Handler> handlers = new HashMap<Method, Handler>();
        for (final ResolvedMethod method : d.getMemberMethods()) {
            final Method raw_method = method.getRawMember();
            final ResolvedType return_type = method.getReturnType();

            if (raw_method.isAnnotationPresent(SqlQuery.class)) {
                if (return_type.isInstanceOf(org.skife.jdbi.v2.Query.class)) {
                    handlers.put(raw_method, new QueryQueryHandler(method));
                }
                else if (return_type.isInstanceOf(List.class)) {
                    handlers.put(raw_method, new ListQueryHandler(method));
                }
                else if (return_type.isInstanceOf(Iterator.class)) {
                    handlers.put(raw_method, new IteratorQueryHandler(method));
                }
                else {
                    handlers.put(raw_method, new SingleValueQueryHandler(method));
                }
            }
            else if (raw_method.isAnnotationPresent(SqlUpdate.class)) {
                handlers.put(raw_method, new UpdateHandler(method));
            }
            else if (method.getName().equals("close") && method.getRawMember().getParameterTypes().length == 0) {
                handlers.put(raw_method, new CloseHandler());
            }
            else if (mixinHandlers.containsKey(raw_method)) {
                handlers.put(raw_method, mixinHandlers.get(raw_method));
            }
            else {
                throw new UnsupportedOperationException("Not Yet Implemented!");
            }

        }
        return handlers;
    }


    private static class UpdateHandler implements Handler
    {
        private final List<Bindifier> binders = new ArrayList<Bindifier>();

        private final String         sql;
        private final ResolvedMethod method;

        public UpdateHandler(ResolvedMethod method)
        {
            this.method = method;
            this.sql = method.getRawMember().getAnnotation(SqlUpdate.class).value();

            Annotation[][] param_annotations = method.getRawMember().getParameterAnnotations();
            for (int param_idx = 0; param_idx < param_annotations.length; param_idx++) {
                Annotation[] annotations = param_annotations[param_idx];
                for (Annotation annotation : annotations) {
                    if (Bind.class.isAssignableFrom(annotation.getClass())) {
                        Bind bind = (Bind) annotation;
                        try {
                            binders.add(new Bindifier(bind, param_idx, bind.binder().newInstance()));
                        }
                        catch (Exception e) {
                            throw new IllegalStateException("unable to instantiate specified binder", e);
                        }
                    }
                }
            }
        }


        public Object invoke(Handle h, Object target, Object[] args)
        {
            Update q = h.createStatement(sql);
            for (Bindifier binder : binders) {
                binder.bind(q, args);
            }
            return q.execute();
        }
    }

    private static class SingleValueQueryHandler extends BaseQueryHandler
    {
        public SingleValueQueryHandler(ResolvedMethod method)
        {
            super(method);
        }

        @Override
        protected Object resultType(org.skife.jdbi.v2.Query q)
        {
            return q.first();
        }

        @Override
        protected ResolvedType mapTo()
        {
            return getMethod().getReturnType();
        }
    }

    private static class IteratorQueryHandler extends BaseQueryHandler
    {
        public IteratorQueryHandler(ResolvedMethod method)
        {
            super(method);
        }

        @Override
        protected Object resultType(org.skife.jdbi.v2.Query q)
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

    private static class ListQueryHandler extends BaseQueryHandler
    {
        public ListQueryHandler(ResolvedMethod method)
        {
            super(method);
        }

        @Override
        protected Object resultType(org.skife.jdbi.v2.Query q)
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

    private static class QueryQueryHandler extends BaseQueryHandler
    {
        public QueryQueryHandler(ResolvedMethod method)
        {
            super(method);
        }

        @Override
        protected Object resultType(org.skife.jdbi.v2.Query q)
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

}
