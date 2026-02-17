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
package org.jdbi.core.internal;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

// Thanks Holger!
// https://stackoverflow.com/questions/35331327/does-java-8-have-cached-support-for-suppliers

/**
 * Wraps a supplier and memoizes the object returned.
 *
 * @param <T> The type of the object returned by this supplier.
 * @see Supplier
 *
 */
public class MemoizingSupplier<T> implements Supplier<T> {
    private final Supplier<T> create;

    private Supplier<T> delegate = this::init;
    private volatile boolean initialized;
    private T value;
    private final ReentrantLock initializationLock;

    private MemoizingSupplier(Supplier<T> create) {
        this.create = create;
        this.initializationLock = new ReentrantLock();
    }

    public static <T> MemoizingSupplier<T> of(Supplier<T> supplier) {
        return new MemoizingSupplier<>(supplier);
    }

    @Override
    public T get() {
        return delegate.get();
    }

    private T internalGet() {
        return value;
    }

    /**
     * Execute a method on the object returned from the supplier if the object was already created.
     * Skips execution if the underlying object was never created.
     *
     * @param consumer A consumer for the object returned by this supplier.
     */
    public void ifInitialized(Consumer<T> consumer) {
        if (initialized) {
            consumer.accept(get());
        }
    }

    private T init() {
        initializationLock.lock();
        try {
            if (!initialized) {
                value = create.get();
                initialized = true;
                delegate = this::internalGet;
            }
            return delegate.get();
        } finally {
            initializationLock.unlock();
        }
    }
}
