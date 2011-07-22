package org.skife.jdbi.v2.sqlobject;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.Handle;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class SqlObject implements InvocationHandler
{
    private static final TypeResolver                               typeResolver  = new TypeResolver();
    private static final Map<Method, Handler>                       mixinHandlers = new HashMap<Method, Handler>();
    private static final ConcurrentMap<Class<?>, Map<Method, Handler>> handlersCache =
        new ConcurrentHashMap<Class<?>, Map<Method, Handler>>();

    static {
        mixinHandlers.putAll(TransactionalHelper.handlers());
        mixinHandlers.putAll(GetHandleHelper.handlers());
        mixinHandlers.putAll(TransmogrifierHelper.handlers());
    }

    @SuppressWarnings("unchecked")
    static <T> T buildSqlObject(final Class<T> sqlObjectType, final HandleDing handle)
    {
        return (T) Proxy.newProxyInstance(sqlObjectType.getClassLoader(),
                                          new Class[]{sqlObjectType, CloseInternal.class},
                                          new SqlObject(buildHandlersFor(sqlObjectType), handle));
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

        try {
            handlers.put(Object.class.getMethod("toString"), new ToStringHandler(sqlObjectType));
            handlers.put(Object.class.getMethod("equals", Object.class), EQUALS);
            handlers.put(Object.class.getMethod("hashCode"), HASHCODE);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("methods on java.lang.Object seem to have gone away. Not good.", e);
        }

        // this is an implicit mixin, not an explicit one, so we need to *always* add it
        handlers.putAll(CloseInternal.Helper.handlers());

        handlers.putAll(EqualsHandler.handler());
        handlers.putAll(ToStringHandler.handler());
        handlers.putAll(HashCodeHandler.handler());

        handlersCache.putIfAbsent(sqlObjectType, handlers);
        return handlers;
    }


    private final Map<Method, Handler> handlers;
    private final HandleDing           ding;

    public SqlObject(Map<Method, Handler> handlers, HandleDing ding)
    {
        this.handlers = handlers;
        this.ding = ding;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        try {
            ding.retain("top-level");
            return handlers.get(method).invoke(ding, proxy, args);
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


    private static Handler HASHCODE = new Handler()
    {
        public Object invoke(HandleDing h, Object target, Object[] args)
        {
            return System.identityHashCode(target);
        }
    };

    private static Handler EQUALS = new Handler() {

        public Object invoke(HandleDing h, Object target, Object[] args)
        {
            return target == args[0];
        }
    };

    private static class ToStringHandler implements Handler
    {
        private final String classname;

        public ToStringHandler(Class<?> sqlObjectType)
        {
            classname = sqlObjectType.getName();
        }

        public Object invoke(HandleDing h, Object target, Object[] args)
        {
            return "{SQL Object from " + classname + " }";
        }
    }
}
