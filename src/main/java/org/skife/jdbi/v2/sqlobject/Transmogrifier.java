/*
 * Copyright 2004 - 2011 Brian McCallister
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.skife.jdbi.v2.sqlobject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Mixin interface which provides a means of creating a sql object from another sql object.
 * The created sql object will obtain handles from the same source as the original.
 */
public interface Transmogrifier
{
    /**
     * Create a new sql object of the specified type, which shares the same means of obtaining
     * handles (or same handle, as the case may be!)
     */
    public <T> T become(Class<T> typeToBecome);


    static class TransformHandler implements Handler
    {
        public Object invoke(HandleDing h, final Object target, Object[] args)
        {
            Class t = (Class) args[0];
            return SqlObject.buildSqlObject(t, h);
        }
    }

    static class Helper
    {
        static Map<Method, Handler> handlers()
        {
            try {
                Map<Method, Handler> h = new HashMap<Method, Handler>();
                h.put(Transmogrifier.class.getMethod("become", Class.class), new TransformHandler());
                return h;
            }
            catch (NoSuchMethodException e) {
                throw new IllegalStateException("someone wonkered up the bytecode", e);
            }
        }
    }
}
