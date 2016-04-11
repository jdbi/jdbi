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
package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;

import java.util.HashSet;
import java.util.Set;

class OnDemandHandleDing implements HandleDing
{
    private final IDBI dbi;
    private final ThreadLocal<LocalDing> threadDing = new ThreadLocal<LocalDing>();

    OnDemandHandleDing(IDBI dbi)
    {
        this.dbi = dbi;
    }

    @Override
    public Handle getHandle()
    {
        if (threadDing.get() == null) {
            threadDing.set(new LocalDing(dbi.open()));
        }
        return threadDing.get().getHandle();
    }

    @Override
    public void retain(String name)
    {
        getHandle(); // need to ensure the local ding has been created as this is called before getHandle sometimes.
        threadDing.get().retain(name);
    }

    @Override
    public void release(String name)
    {
        LocalDing ding = threadDing.get();
        if (ding == null) {
            return;
        }
        ding.release(name);

    }

    class LocalDing implements HandleDing {

        private final Set<String> retentions = new HashSet<String>();
        private final Handle handle;

        LocalDing(Handle handle)
        {
            this.handle = handle;
        }

        @Override
        public Handle getHandle()
        {
            return handle;
        }

        @Override
        public void release(String name)
        {
            retentions.remove(name);
            if (retentions.isEmpty()) {
                threadDing.set(null);
                handle.close();
            }
        }

        @Override
        public void retain(String name)
        {
            retentions.add(name);
        }

    }
}
