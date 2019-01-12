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
import java.util.EnumSet;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.jdbi.v3.core.generic.GenericType;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

public class EnumSetCollectorFactoryTest {

    private EnumSetCollectorFactory factory = new EnumSetCollectorFactory();

    @Test
    public void enumSet() {
        GenericType genericType = new GenericType<EnumSet<Color>>() {};
        Type containerType = genericType.getType();
        Class<?> erasedType = getErasedType(containerType);
        assertThat(factory.accepts(containerType)).isTrue();
        assertThat(factory.accepts(erasedType)).isFalse();
        assertThat(factory.elementType(containerType)).contains(Color.class);

        Collector<Color, ?, EnumSet<Color>> collector = (Collector<Color, ?, EnumSet<Color>>) factory.build(containerType);
        assertThat(Stream.of(Color.RED, Color.BLUE).collect(collector))
            .isInstanceOf(erasedType)
            .containsExactly(Color.RED, Color.BLUE);
    }

    /**
     * For EnumSet test only.
     */
    private enum Color {
        RED,
        GREEN,
        BLUE
    }
}
