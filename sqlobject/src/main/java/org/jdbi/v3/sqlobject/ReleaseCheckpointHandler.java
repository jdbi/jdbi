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
import java.util.function.Supplier;

import org.jdbi.v3.core.Handle;

class ReleaseCheckpointHandler implements Handler
{
    @Override
    public Object invoke(Supplier<Handle> handle, SqlObjectConfig config, Object target, Object[] args, Method method)
    {
        handle.get().release(String.valueOf(args[0]));
        return null;
    }
}
