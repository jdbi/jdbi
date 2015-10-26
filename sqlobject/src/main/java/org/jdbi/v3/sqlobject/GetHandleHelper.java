/*
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
package org.jdbi.v3.sqlobject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.jdbi.v3.sqlobject.mixins.GetHandle;
import org.jdbi.v3.tweak.HandleCallback;
import org.jdbi.v3.tweak.HandleConsumer;

class GetHandleHelper
{
    static Map<Method, Handler> handlers()
    {
        try {
            Map<Method, Handler> h = new HashMap<Method, Handler>();
            h.put(GetHandle.class.getMethod("getHandle"), new GetHandleHandler());
            h.put(GetHandle.class.getMethod("withHandle", HandleCallback.class), new WithHandleHandler());
            h.put(GetHandle.class.getMethod("useHandle", HandleConsumer.class), new UseHandleHandler());
            return h;
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("someone wonkered up the bytecode", e);
        }

    }
}
