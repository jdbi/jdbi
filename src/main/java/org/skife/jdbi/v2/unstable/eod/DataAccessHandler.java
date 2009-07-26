/*
 * Copyright 2004-2007 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2.unstable.eod;

import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.Update;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;

class DataAccessHandler implements InvocationHandler
{
    private final Handle handle;

    public DataAccessHandler(Handle handle)
    {
        this.handle = handle;
    }

    private void bindArguments(BindType type, SQLStatement<?> sql, Object... args)
    {
        switch (type)
        {
            case Bean:
            {
                sql.bindFromProperties(args[0]);
                break;
            }
            case Position:
            {
                for (int i = 0; i < args.length; i++)
                {
                    Object arg = args[i];
                    sql.bind(i, arg);
                }
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Object execute(Select s, Method method, Object[] args, Foo f)
    {
        Type t = method.getGenericReturnType();
        assert(t instanceof ParameterizedType);
        ParameterizedType returnType = (ParameterizedType) t;
        final Type[] h = returnType.getActualTypeArguments();
        assert(h.length == 1);
        assert(h[0] instanceof Class);
        Class c = (Class) h[0];
        Query q = handle.createQuery(s.value()).map(c);
        if (args != null) bindArguments(method.isAnnotationPresent(BindBy.class) ?
                                        method.getAnnotation(BindBy.class).value() :
                                        BindType.Position, q, args);
        return f.doit(q);
    }

    @SuppressWarnings("unchecked")
    private Object handleInstance(Select s, Method method, Object[] args)
    {
        Query q = handle.createQuery(s.value()).map(method.getReturnType());
        if (args != null) bindArguments(method.isAnnotationPresent(BindBy.class) ?
                                        method.getAnnotation(BindBy.class).value() :
                                        BindType.Position, q, args);
        if (args != null) bindArguments(method.isAnnotationPresent(BindBy.class) ?
                                        method.getAnnotation(BindBy.class).value() :
                                        BindType.Position, q, args);
        q.setMaxRows(1);
        return q.first();
    }

    private Object handleDML(String s, Method method, Object[] args)
    {
        Update u = handle.createStatement(s);
        if (args != null) bindArguments(method.isAnnotationPresent(BindBy.class) ?
                                        method.getAnnotation(BindBy.class).value() :
                                        BindType.Position, u, args);
        Class<?> rt = method.getReturnType();
        int changed = u.execute();
        if (method.getReturnType() == null)
        {
            return null;
        }
        else if (Integer.class.equals(rt) || int.class.equals(rt))
        {
            return changed;
        }
        else if (Boolean.class.equals(rt) || boolean.class.equals(rt))
        {
            return changed != 0;
        }
        return null;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        if (method.getDeclaringClass().equals(DataAccessor.class))
        {
            this.handle.close();
            return null;
        }
        else if (method.isAnnotationPresent(Select.class))
        {
            final Select s = method.getAnnotation(Select.class);
            final Class<?> returnTypeClass = method.getReturnType();
            if (List.class.isAssignableFrom(returnTypeClass))
            {
                return this.execute(s, method, args, LISTIFIER);
            }
            if (Iterator.class.isAssignableFrom(returnTypeClass))
            {
                return this.execute(s, method, args, ITTIFIER);
            }
            return this.handleInstance(s, method, args);
        }
        else if (method.isAnnotationPresent(Insert.class))
        {
            Insert i = method.getAnnotation(Insert.class);
            return this.handleDML(i.value(), method, args);
        }
        else if (method.isAnnotationPresent(org.skife.jdbi.v2.unstable.eod.Update.class))
        {
            org.skife.jdbi.v2.unstable.eod.Update i = method.getAnnotation(org.skife.jdbi.v2.unstable.eod.Update.class);
            return this.handleDML(i.value(), method, args);
        }
        else if (method.isAnnotationPresent(Delete.class))
        {
            Delete i = method.getAnnotation(Delete.class);
            return this.handleDML(i.value(), method, args);
        }
        throw new UnsupportedOperationException("Not Yet Implemented!");
    }

    private interface Foo
    {
        Object doit(Query<?> q);
    }

    private static final Foo LISTIFIER = new Foo()
    {
        public Object doit(Query<?> q)
        {
            return q.list();
        }
    };

    private static final Foo ITTIFIER = new Foo()
    {
        public Object doit(Query<?> q)
        {
            return q.iterator();
        }
    };
}
