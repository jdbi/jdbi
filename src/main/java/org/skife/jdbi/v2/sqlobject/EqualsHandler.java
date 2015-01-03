/*
 * Copyright (C) 2004 - 2014 Brian McCallister
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
package org.skife.jdbi.v2.sqlobject;

import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

class EqualsHandler implements Handler
{
    @Override
    public Object invoke(final HandleDing h, final Object target, final Object[] args, MethodProxy mp)
    {
        // basic reference equals for now.
        return target == args[0];
    }

    static Map<Method, Handler> handler()
    {
        try
        {
            Map<Method, Handler> handler = new HashMap<Method, Handler>();
            handler.put(Object.class.getMethod("equals", Object.class), new EqualsHandler());
            return handler;
        }
        catch (NoSuchMethodException e)
        {
            throw new IllegalStateException("JVM error");
        }
    }

}
