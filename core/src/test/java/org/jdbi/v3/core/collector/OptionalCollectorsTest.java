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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.junit.Test;

public class OptionalCollectorsTest {
  @Test
  public void toOptional() {
    Collector<String, ?, Optional<String>> collector = OptionalCollectors.toOptional();

    assertThat(Stream.<String>empty().collect(collector)).isEmpty();

    assertThat(Stream.of((String) null).collect(collector)).isEmpty();
    assertThat(Stream.of("foo").collect(collector)).hasValue("foo");

    assertThatThrownBy(
        () -> Stream.of("foo", "bar").collect(collector))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Multiple values for optional: ['foo', 'bar', ...]");
    assertThatThrownBy(
        () -> Stream.of("foo", null).collect(collector))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Multiple values for optional: ['foo', null, ...]");
    assertThatThrownBy(
        () -> Stream.of(null, "bar").collect(collector))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Multiple values for optional: [null, 'bar', ...]");
  }

  @Test
  public void toOptionalInt() {
    Collector<Integer, ?, OptionalInt> collector = OptionalCollectors.toOptionalInt();

    assertThat(Stream.<Integer>empty().collect(collector)).isEmpty();

    assertThat(Stream.of((Integer) null).collect(collector)).isEmpty();
    assertThat(Stream.of(1).collect(collector)).hasValue(1);

    assertThatThrownBy(
        () -> Stream.of(1, 2).collect(collector))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Multiple values for optional: ['1', '2', ...]");
    assertThatThrownBy(
        () -> Stream.of(1, null).collect(collector))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Multiple values for optional: ['1', null, ...]");
    assertThatThrownBy(
        () -> Stream.of(null, 2).collect(collector))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Multiple values for optional: [null, '2', ...]");
  }

  @Test
  public void toOptionalLong() {
    Collector<Long, ?, OptionalLong> collector = OptionalCollectors.toOptionalLong();

    assertThat(Stream.<Long>empty().collect(collector)).isEmpty();

    assertThat(Stream.of((Long) null).collect(collector)).isEmpty();
    assertThat(Stream.of(1L).collect(collector)).hasValue(1L);

    assertThatThrownBy(
        () -> Stream.of(1L, 2L).collect(collector))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Multiple values for optional: ['1', '2', ...]");
    assertThatThrownBy(
        () -> Stream.of(1L, null).collect(collector))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Multiple values for optional: ['1', null, ...]");
    assertThatThrownBy(
        () -> Stream.of(null, 2L).collect(collector))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Multiple values for optional: [null, '2', ...]");
  }

  @Test
  public void toOptionalDouble() {
    Collector<Double, ?, OptionalDouble> collector = OptionalCollectors.toOptionalDouble();

    assertThat(Stream.<Double>empty().collect(collector)).isEmpty();

    assertThat(Stream.of((Double) null).collect(collector)).isEmpty();
    assertThat(Stream.of(1.5d).collect(collector)).hasValue(1.5d);

    assertThatThrownBy(
        () -> Stream.of(1.5d, 2.25d).collect(collector))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Multiple values for optional: ['1.5', '2.25', ...]");
    assertThatThrownBy(
        () -> Stream.of(1.5d, null).collect(collector))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Multiple values for optional: ['1.5', null, ...]");
    assertThatThrownBy(
        () -> Stream.of(null, 2.25d).collect(collector))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Multiple values for optional: [null, '2.25', ...]");
  }
}
