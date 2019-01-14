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
package org.jdbi.v3.core.collector;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.jdbi.v3.core.generic.GenericType;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OptionalCollectorFactoryTest {
    private CollectorFactory factory = new OptionalCollectorFactory();

    @Test
    public void optional() {
        Type optionalString = new GenericType<Optional<String>>() {}.getType();
        assertThat(factory.accepts(optionalString)).isTrue();
        assertThat(factory.accepts(Optional.class)).isFalse();
        assertThat(factory.elementType(optionalString)).contains(String.class);

        Collector<String, ?, Optional<String>> collector = (Collector<String, ?, Optional<String>>) factory.build(optionalString);
        assertThat(Stream.<String>empty().collect(collector)).isEmpty();
        assertThat(Stream.of("foo").collect(collector)).contains("foo");
        assertThatThrownBy(() -> Stream.of("foo", "bar").collect(collector))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Multiple values");
    }
}
