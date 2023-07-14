/*
 * Copyright (c), Data Geekery GmbH, contact@datageekery.com
 *
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
package org.jdbi.v3.core.internal.exceptions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.jdbi.v3.core.internal.UtilityClassException;

public class Sneaky {

    private Sneaky() {
        throw new UtilityClassException();
    }

    /**
     * Will <b>always</b> throw an exception, so the caller should also always throw the dummy return value to make sure the control flow remains clear.
     */
    @CheckReturnValue
    @NonNull
    public static DummyException throwAnyway(Throwable t) {

        if (t instanceof Error) {
            throw (Error) t;
        } else if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else if (t instanceof IOException) {
            throw new UncheckedIOException((IOException) t);
        } else if (t instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        } else if (t instanceof InvocationTargetException) {
            throw throwAnyway(t.getCause());
        }

        throw throwEvadingChecks(t);
    }

    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    private static <E extends Throwable> E throwEvadingChecks(Throwable throwable) throws E {
        throw (E) throwable;
    }
}
