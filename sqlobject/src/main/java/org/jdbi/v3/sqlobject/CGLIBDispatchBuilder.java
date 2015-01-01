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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.NoOp;

class CGLIBDispatchBuilder
{
    private final SortedMap<Method, Callback> callmap;
    private final Callback defalt;

    private CGLIBDispatchBuilder(Callback defalt, SortedMap<Method, Callback> callmap)
    {
        this.callmap = callmap;
        this.defalt = defalt;
    }

    public static CGLIBDispatchBuilder create()
    {
        return new CGLIBDispatchBuilder(NoOp.INSTANCE, new TreeMap<Method, Callback>(MC));
    }

    public CGLIBDispatchBuilder withDefault(Callback defalt)
    {
        return new CGLIBDispatchBuilder(defalt, this.callmap);
    }

    public CGLIBDispatchBuilder addCallbacks(Map<Method, Callback> additions)
    {
        TreeMap<Method, Callback> newmap = new TreeMap<Method, Callback>(MC);
        newmap.putAll(this.callmap);
        newmap.putAll(additions);
        return new CGLIBDispatchBuilder(this.defalt, newmap);
    }

    public CGLIBDispatchBuilder addCallback(Method method, Callback callback)
    {
        TreeMap<Method, Callback> newmap = new TreeMap<Method, Callback>(MC);
        newmap.putAll(this.callmap);
        newmap.put(method, callback);
        return new CGLIBDispatchBuilder(this.defalt, newmap);
    }

    public Callback[] getCallbacks()
    {
        Callback[] callbacks = new Callback[callmap.size() + 1];
        callbacks[callmap.size()] = defalt;
        int idx = 0;
        for (Callback callback : callmap.values()) {
            callbacks[idx++] = callback;
        }
        return callbacks;
    }

    public CallbackFilter getFilter()
    {
        final Map<Method, Integer> indices = new LinkedHashMap<Method, Integer>();
        int idx = 0;
        for (Method method : callmap.keySet()) {
            indices.put(method, idx++);
        }
        final int defalt_idx = idx;
        return new MyCallbackFilter(indices, defalt_idx);
    }

    private static final Comparator<Method> MC = new Comparator<Method>()
    {
        @Override
        public int compare(Method left, Method right)
        {
            return left.toString().compareTo(right.toString());
        }
    };

    private static class MyCallbackFilter implements CallbackFilter
    {
        private final Map<Method, Integer> indices;
        private final int defalt_idx;

        public MyCallbackFilter(Map<Method, Integer> indices, int defalt_idx)
        {
            this.indices = indices;
            this.defalt_idx = defalt_idx;
        }

        @Override
        public int accept(Method method)
        {
            if (indices.containsKey(method)) {
                return indices.get(method);
            }
            else {
                return defalt_idx;
            }
        }

        @Override
        public int hashCode()
        {
            return indices.hashCode() + defalt_idx;
        }

        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof MyCallbackFilter)) {
                return false;
            }
            MyCallbackFilter other = (MyCallbackFilter) o;
            return this.indices.equals(other.indices) && this.defalt_idx == other.defalt_idx;
        }
    }
}
