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

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class JdbiInvocationWrappers {
    private JdbiInvocationWrappers() {
        throw new UtilityClassException();
    }

    public static <V, L> V setAndRevert(ThreadLocal<L> backing, L context, Callable<V> task) throws Exception {
        return setAndRevert(
            backing::get,
            value -> {
                if (value == null) {
                    backing.remove();
                } else {
                    backing.set(value);
                }
            },
            context,
            task
        );
    }

    public static <V, L> V setAndRevert(Supplier<L> initial, Consumer<L> becoming, L taskContext, Callable<V> task) throws Exception {
        L original = initial.get();
        try {
            becoming.accept(taskContext);
            return task.call();
        } finally {
            becoming.accept(original);
        }
    }
}
