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
package org.jdbi.v3.tweak;

import org.jdbi.v3.Handle;

/**
 * Abstract {@link HandleCallback} that doesn't return a result.
 */
public abstract class VoidHandleCallback implements HandleCallback<Void>
{
    /**
     * This implementation delegates to {@link #execute}.
     *
     * @param handle {@inheritDoc}
     * @return nothing
     * @throws Exception {@inheritDoc}
     */
    @Override
    public final Void withHandle(Handle handle) throws Exception
    {
        execute(handle);
        return null;
    }

    /**
     * {@link #withHandle} will delegate to this method.
     *
     * @param handle Handle to be used only within scope of this callback
     * @throws Exception will result in a {@link org.jdbi.v3.exceptions.CallbackFailedException} wrapping
     *                   the exception being thrown
     */
    protected abstract void execute(Handle handle) throws Exception;
}
