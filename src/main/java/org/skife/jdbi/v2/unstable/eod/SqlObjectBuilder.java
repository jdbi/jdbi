package org.skife.jdbi.v2.unstable.eod;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public class SqlObjectBuilder
{
    private final static TypeResolver tr = new TypeResolver();

    private final DBI dbi;

    public SqlObjectBuilder(DBI dbi)
    {
        this.dbi = dbi;
    }

    public <T extends Closeable> T open(Class<T> sqlObjectType)
    {
        final ResolvedType sql_object_type = tr.resolve(sqlObjectType);

        MemberResolver mr = new MemberResolver(tr);
        ResolvedTypeWithMembers d = mr.resolve(sql_object_type, null, null);
        final Map<Method, Handler> handlers = new HashMap<Method, Handler>();
        for (ResolvedMethod method : d.getMemberMethods()) {
            if (method.getRawMember().isAnnotationPresent(Sql.class)) {
                handlers.put(method.getRawMember(), new SqlHandler(method));
            }
            else if (method.getName().equals("close") && method.getRawMember().getParameterTypes().length == 0) {
                handlers.put(method.getRawMember(), new CloseHandler());
            }
        }

        final Handle handle = dbi.open();
        final InvocationHandler handler = new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
                return handlers.get(method).invoke(handle, args);
            }
        };


        return (T) Proxy.newProxyInstance(sqlObjectType.getClassLoader(), new Class[] {sqlObjectType}, handler);
    }
}
