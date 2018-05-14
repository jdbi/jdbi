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

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.jdbi.v3.core.internal.JdbiStreams.toStream;

public class JdbiOptionals {
    private JdbiOptionals() {
        throw new UnsupportedOperationException("utility class");
    }

    @SafeVarargs
    public static <T> Optional<T> findFirstPresent(Supplier<Optional<T>>... suppliers) {
        return Stream.of(suppliers)
                .flatMap(supplier -> toStream(supplier.get()))
                .findFirst();
    }
}
