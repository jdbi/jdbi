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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.jdbi.v3.core.generic.GenericType;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

public class SetCollectorFactoryTest {
    private CollectorFactory factory = new SetCollectorFactory();

    @Test
    public void collections() {
        testCollectionType(new GenericType<Set<String>>(){});
        testCollectionType(new GenericType<HashSet<String>>(){});
        testCollectionType(new GenericType<SortedSet<String>>(){});
        testCollectionType(new GenericType<TreeSet<String>>(){});
    }

    private <C extends Collection<String>> void testCollectionType(GenericType<C> genericType) {
        Type containerType = genericType.getType();
        Class<?> erasedType = getErasedType(containerType);
        assertThat(factory.accepts(containerType)).isTrue();
        assertThat(factory.accepts(erasedType)).isFalse();
        assertThat(factory.elementType(containerType)).contains(String.class);

        Collector<String, ?, C> collector = (Collector<String, ?, C>) factory.build(containerType);
        assertThat(Stream.of("foo", "bar", "baz").collect(collector))
            .isInstanceOf(erasedType)
            .containsOnly("foo", "bar", "baz");
    }
}
