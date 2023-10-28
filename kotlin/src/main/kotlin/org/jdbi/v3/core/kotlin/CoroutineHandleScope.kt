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
package org.jdbi.v3.core.kotlin

import kotlinx.coroutines.CopyableThreadContextElement
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.HandleScope
import org.jdbi.v3.core.extension.HandleSupplier
import org.jdbi.v3.core.internal.ThreadLocalHandleScope
import kotlin.coroutines.CoroutineContext

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
internal class CoroutineHandleScope private constructor(
    var handle: Handle?,
    private val delegate: HandleScope
) : CopyableThreadContextElement<HandleSupplier?>, HandleScope {

    override val key: CoroutineContext.Key<CoroutineHandleScope> = Key

    constructor() : this(handle = null, delegate = ThreadLocalHandleScope())

    override fun updateThreadContext(context: CoroutineContext): HandleSupplier? {
        val oldState: HandleSupplier? = delegate.get()

        // if a handle was set, use that handle instead of the current (which comes
        // out of the thread local). Otherwise, clear out the state (coroutine then
        // creates a new handle)
        if (handle == null) {
            delegate.clear()
        } else {
            delegate.set(handle)
        }
        return oldState
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: HandleSupplier?) {
        if (oldState == null) {
            delegate.clear()
        } else {
            delegate.set(oldState)
        }
    }

    // Copy from the ThreadLocal source of truth at child coroutine launch time. This makes
    // ThreadLocal writes between resumption of the parent coroutine and the launch of the
    // child coroutine visible to the child.
    override fun copyForChild(): CoroutineHandleScope = CoroutineHandleScope(handle = handle, delegate = delegate)

    override fun mergeForChild(overwritingElement: CoroutineContext.Element): CoroutineContext {
        // Merge operation defines how to handle situations when both
        // the parent coroutine has an element in the context and
        // an element with the same key was also
        // explicitly passed to the child coroutine.
        // If merging does not require special behavior,
        // the copy of the element can be returned.
        return if (overwritingElement is CoroutineHandleScope) {
            overwritingElement.copyForChild()
        } else {
            this
        }
    }

    override fun get(): HandleSupplier? = delegate.get()

    override fun set(handleSupplier: HandleSupplier?) {
        delegate.set(handleSupplier)
    }

    override fun clear() {
        delegate.clear()
    }

    companion object Key : CoroutineContext.Key<CoroutineHandleScope>
}
