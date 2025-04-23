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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Extend the {@link ResultIterable} for batch operations.
 * @param <T> The generic type for the iterable.
 */
public interface BatchResultIterable<T> extends ResultIterable<T> {

    /**
     * Split the results into per-batch sub-lists. Note that this may not be correct if any of the executed batches returned an error code.
     *
     * @return results in a {@link List} of {@link List}s.
     */
    List<List<T>> listPerBatch();

    static <U> BatchResultIterable<U> of(ResultIterable<U> delegate, Supplier<int[]> modifiedRowCountsSupplier) {
        return new BatchResultIterable<>() {
            @Override
            public List<List<U>> listPerBatch() {
                List<List<U>> results = new ArrayList<>();
                try (ResultIterator<U> iterator = delegate.iterator()) {
                    for (int modCount : modifiedRowCountsSupplier.get()) {
                        if (modCount <= 0) {
                            // error return (SUCCESS_NO_INFO or EXECUTE_FAILED) or empty.
                            results.add(Collections.emptyList());
                        } else {
                            List<U> batchResult = new ArrayList<>(modCount);
                            for (int i = 0; i < modCount; i++) {
                                batchResult.add(iterator.next());
                            }
                            results.add(batchResult);
                        }
                    }
                }
                return results;
            }

            @Override
            public ResultIterator<U> iterator() {
                return delegate.iterator();
            }
        };
    }
}
