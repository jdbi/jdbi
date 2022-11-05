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
package org.jdbi.v3.core.internal.exceptions;

import java.util.LinkedList;
import java.util.function.Function;

import org.jdbi.v3.core.internal.exceptions.Unchecked.CheckedRunnable;

import static org.jdbi.v3.core.internal.exceptions.Sneaky.throwAnyway;

/**
 * Manage multi-exception chains by adding additional throwables as suppressed.
 */
public final class ThrowableSuppressor {

    private final LinkedList<Throwable> throwables = new LinkedList<>();

    /**
     * Run a piece of code and record any thrown exception at the end of the exception chain.
     *
     * @param supplier     Code to execute.
     * @param defaultValue Default value if the code throws an exception.
     * @param <T>          The return value type.
     * @return The output of the executed code or the default value in case of an exception.
     */
    public <T> T suppressAppend(CheckedSupplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Throwable t) {
            throwables.add(t);
            return defaultValue;
        }
    }

    /**
     * Run a piece of code and record any thrown exception at the end of the exception chain.
     *
     * @param runnable Code to execute.
     */
    public void suppressAppend(CheckedRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            throwables.add(t);
        }
    }

    /**
     * Run a piece of code and record any thrown exception at the beginning of the exception chain.
     *
     * @param supplier     Code to execute.
     * @param defaultValue Default value if the code throws an exception.
     * @param <T>          The return value type.
     * @return The output of the executed code or the default value in case of an exception.
     */
    public <T> T suppressPrepend(CheckedSupplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Throwable t) {
            throwables.addFirst(t);
            return defaultValue;
        }
    }

    /**
     * Run a piece of code and record any thrown exception at the beginning of the exception chain.
     *
     * @param runnable Code to execute.
     */
    public void suppressPrepend(CheckedRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            throwables.addFirst(t);
        }
    }

    /**
     * Attach all recorded exceptions to another throwable as suppressed exceptions.
     *
     * @param throwable The throwable to attach to.
     */
    public void attachToThrowable(Throwable throwable) {
        for (Throwable t : throwables) {
            throwable.addSuppressed(t);
        }
        throwables.clear();
    }

    /**
     * Attach all recorded exceptions to a new throwable and throw it.
     *
     * @param function Returns the throwable to attach to. Only called if there are any additional throwables present. The first one present is passed into the
     *                 function and is intended as the cause for the exception chain.
     */
    public <T extends Throwable> void throwIfNecessary(Function<Throwable, T> function) throws T {
        if (!throwables.isEmpty()) {
            T result = function.apply(throwables.removeFirst());
            attachToThrowable(result);
            throw result;
        }
    }

    /**
     * If any throwables were recorded, use the first as the primary exception and attach all other suppressed exceptions to it.
     */
    public void throwIfNecessary() {
        if (!throwables.isEmpty()) {
            Throwable first = throwables.removeFirst();
            attachToThrowable(first);
            throw throwAnyway(first);
        }
    }
}
