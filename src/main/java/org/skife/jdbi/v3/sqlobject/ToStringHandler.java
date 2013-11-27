/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v3.sqlobject;

import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

class ToStringHandler implements Handler
{
    private final String className;

    ToStringHandler(String className)
    {
        this.className = className;
    }

    public Object invoke(final HandleDing h, final Object target, final Object[] args, MethodProxy mp)
    {
        return className + '@' + Integer.toHexString(target.hashCode());
    }

    static Map<Method, Handler> handler(String className)
    {
        try
        {
            Map<Method, Handler> handler = new HashMap<Method, Handler>();
            handler.put(Object.class.getMethod("toString"), new ToStringHandler(className));
            return handler;
        }
        catch (NoSuchMethodException e)
        {
            throw new IllegalStateException("JVM error");
        }
    }

}
