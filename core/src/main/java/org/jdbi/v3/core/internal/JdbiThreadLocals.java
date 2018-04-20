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

public class JdbiThreadLocals {
    private JdbiThreadLocals() {
        throw new UnsupportedOperationException("utility class");
    }

    public static <V, L> V invokeInContext(ThreadLocal<L> threadLocal, L context, Callable<V> task) throws Exception {
        L oldContext = threadLocal.get();
        try {
            threadLocal.set(context);
            return task.call();
        }
        finally {
            if (oldContext == null) {
                threadLocal.remove();
            }
            else {
                threadLocal.set(oldContext);
            }
        }
    }
}
