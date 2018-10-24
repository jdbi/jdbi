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

import java.io.IOException;
import java.io.UncheckedIOException;

public class Throwables {
    private Throwables() {
        throw new UnsupportedOperationException("utility class");
    }

    /**
     * conservatively unchecks {@code t} and throws it.
     *
     * Returns a bogus exception for compiler satisfaction: caller should always throw the return value even though you'll never get it.
     */
    public static RuntimeException throwOnlyUnchecked(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }

        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }

        if (t instanceof IOException) {
            throw new UncheckedIOException((IOException) t);
        }

        throw new RuntimeException(t);
    }

    /**
     * executes a runnable that could throw a Throwable and conservatively unchecks it so you don't need to catch anything.
     */
    public static <T> T throwingOnlyUnchecked(DangerousRunnable<T> dangerous) {
        try {
            return dangerous.run();
        } catch (Throwable t) {
            throw throwOnlyUnchecked(t);
        }
    }

    public interface DangerousRunnable<T> {
        T run() throws Throwable;
    }
}
