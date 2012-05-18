package org.skife.jdbi.v2.sqlobject;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedMethod;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.springframework.transaction.config.TxNamespaceHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class SqlObject
{
    private static final TypeResolver                                  typeResolver  = new TypeResolver();
    private static final Map<Method, Handler>                          mixinHandlers = new HashMap<Method, Handler>();
    private static final ConcurrentMap<Class<?>, Map<Method, Handler>> handlersCache = new ConcurrentHashMap<Class<?>, Map<Method, Handler>>();
    private static final ConcurrentMap<Class<?>, Factory>              factories     = new ConcurrentHashMap<Class<?>, Factory>();

    static {
        mixinHandlers.putAll(TransactionalHelper.handlers());
        mixinHandlers.putAll(GetHandleHelper.handlers());
        mixinHandlers.putAll(TransmogrifierHelper.handlers());
    }

    @SuppressWarnings("unchecked")
    static <T> T buildSqlObject(final Class<T> sqlObjectType, final HandleDing handle)
    {
        Factory f;
        if (factories.containsKey(sqlObjectType)) {
            f = factories.get(sqlObjectType);
        }
        else {
            Enhancer e = new Enhancer();
            List<Class> interfaces = new ArrayList<Class>();
            interfaces.add(CloseInternal.class);
            if (sqlObjectType.isInterface()) {
                interfaces.add(sqlObjectType);
            }
            else {
                e.setSuperclass(sqlObjectType);
            }
            e.setInterfaces(interfaces.toArray(new Class[interfaces.size()]));
            final SqlObject so = new SqlObject(buildHandlersFor(sqlObjectType), handle);
            e.setCallback(new MethodInterceptor()
            {
                @Override
                public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable
                {
                    return so.invoke(o, method, objects, methodProxy);
                }
            });
            T t = (T) e.create();
            factories.putIfAbsent(sqlObjectType, (Factory) t);
            return t;
        }

        final SqlObject so = new SqlObject(buildHandlersFor(sqlObjectType), handle);
        return (T) f.newInstance(new MethodInterceptor()
        {
            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable
            {
                return so.invoke(o, method, objects, methodProxy);
            }
        });
    }

    private static Map<Method, Handler> buildHandlersFor(Class<?> sqlObjectType)
    {
        if (handlersCache.containsKey(sqlObjectType)) {
            return handlersCache.get(sqlObjectType);
        }

        final MemberResolver mr = new MemberResolver(typeResolver);
        final ResolvedType sql_object_type = typeResolver.resolve(sqlObjectType);

        final ResolvedTypeWithMembers d = mr.resolve(sql_object_type, null, null);

        final Map<Method, Handler> handlers = new HashMap<Method, Handler>();
        for (final ResolvedMethod method : d.getMemberMethods()) {
            final Method raw_method = method.getRawMember();

            if (raw_method.isAnnotationPresent(SqlQuery.class)) {
                handlers.put(raw_method, new QueryHandler(sqlObjectType, method, ResultReturnThing.forType(method)));
            }
            else if (raw_method.isAnnotationPresent(SqlUpdate.class)) {
                handlers.put(raw_method, new UpdateHandler(sqlObjectType, method));
            }
            else if (raw_method.isAnnotationPresent(SqlBatch.class)) {
                handlers.put(raw_method, new BatchHandler(sqlObjectType, method));
            }
            else if (raw_method.isAnnotationPresent(SqlCall.class)) {
                handlers.put(raw_method, new CallHandler(sqlObjectType, method));
            }
            else if (method.getName().equals("close") && method.getRawMember().getParameterTypes().length == 0) {
                handlers.put(raw_method, new CloseHandler());
            }
            else if (raw_method.isAnnotationPresent(Transaction.class)) {
                handlers.put(raw_method, new PassThroughTransactionHandler(raw_method, raw_method.getAnnotation(Transaction.class)));
            }
            else if (mixinHandlers.containsKey(raw_method)) {
                handlers.put(raw_method, mixinHandlers.get(raw_method));
            }
            else {
                throw new IllegalArgumentException("Method " + raw_method.getDeclaringClass()
                                                                         .getName() + "#" + raw_method.getName() + " doesn't make sense -- it probably needs a @Sql* annotation of some kind.");
            }
        }

        // this is an implicit mixin, not an explicit one, so we need to *always* add it
        handlers.putAll(CloseInternal.Helper.handlers());

        handlers.putAll(EqualsHandler.handler());
        handlers.putAll(ToStringHandler.handler(sqlObjectType.getName()));
        handlers.putAll(HashCodeHandler.handler());

        return handlers;
    }


    private final Map<Method, Handler> handlers;
    private final HandleDing           ding;

    public SqlObject(Map<Method, Handler> handlers, HandleDing ding)
    {
        this.handlers = handlers;
        this.ding = ding;
    }

    public Object invoke(Object proxy, Method method, Object[] args, MethodProxy mp) throws Throwable
    {
        try {
            ding.retain("top-level");
            return handlers.get(method).invoke(ding, proxy, args, mp);
        }
        finally {
            ding.release("top-level");
        }
    }

    public static void close(Object sqlObject)
    {
        if (!(sqlObject instanceof CloseInternal)) {
            throw new IllegalArgumentException(sqlObject + " is not a sql object");
        }
        CloseInternal closer = (CloseInternal) sqlObject;
        closer.___jdbi_close___();
    }

    static String getSql(SqlCall q, Method m)
    {
        if (SqlQuery.DEFAULT_VALUE.equals(q.value())) {
            return m.getName();
        }
        else {
            return q.value();
        }
    }

    static String getSql(SqlQuery q, Method m)
    {
        if (SqlQuery.DEFAULT_VALUE.equals(q.value())) {
            return m.getName();
        }
        else {
            return q.value();
        }
    }

    static String getSql(SqlUpdate q, Method m)
    {
        if (SqlQuery.DEFAULT_VALUE.equals(q.value())) {
            return m.getName();
        }
        else {
            return q.value();
        }
    }

    static String getSql(SqlBatch q, Method m)
    {
        if (SqlQuery.DEFAULT_VALUE.equals(q.value())) {
            return m.getName();
        }
        else {
            return q.value();
        }
    }
}
