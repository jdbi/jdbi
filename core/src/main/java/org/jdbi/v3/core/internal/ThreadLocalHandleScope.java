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
package org.jdbi.v3.core.internal;

import org.jdbi.v3.core.HandleScope;
import org.jdbi.v3.core.extension.HandleSupplier;

import static java.util.Objects.requireNonNull;

/**
 * The default implementation for {@link HandleScope}. Uses a {@link ThreadLocal} to manage per-thread scope.
 */
public final class ThreadLocalHandleScope implements HandleScope {

    private final ThreadLocal<HandleSupplier> threadLocal = new ThreadLocal<>();

    @Override
    public HandleSupplier get() {
        return threadLocal.get();
    }

    @Override
    public void set(HandleSupplier handleSupplier) {
        requireNonNull(handleSupplier, "handleSupplier is null");
        threadLocal.set(handleSupplier);
    }

    @Override
    public void clear() {
        threadLocal.remove();
    }
}
