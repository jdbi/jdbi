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
package org.jdbi.v3.core.result;

import java.io.Closeable;
import java.util.Iterator;
import java.util.function.Consumer;

import org.jdbi.v3.core.statement.StatementContext;

/**
 * Represents a forward-only iterator over a result set, which will lazily iterate
 * the results. The underlying <code>ResultSet</code> can be closed by calling the
 * {@link ResultIterator#close()} method.
 * <p>
 * The default implementation of <code>ResultIterator</code> will automatically close
 * the result set after the last element has been retrieved via <code>next()</code> and
 * <code>hasNext()</code> is called (which will return false). This allows for iteration
 * over the results with automagic resource cleanup.
 * <p>
 * The <code>remove()</code> operation is not supported in the default
 * version, and will raise an <code>UnsupportedOperationException</code>
 */
public interface ResultIterator<T> extends Iterator<T>, Closeable {
    @Override
    default void forEachRemaining(Consumer<? super T> action) {
        try {
            Iterator.super.forEachRemaining(action);
        } finally {
            close();
        }
    }

    /**
     * Close the underlying result set.
     */
    @Override
    void close();

    /**
     * @return the current statement context
     */
    StatementContext getContext();
}
