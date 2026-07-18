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

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrationListsTest {

    @Test
    void prependPutsHeadFirstWithoutMutatingBase() {
        List<String> base = List.of("a", "b");

        List<String> result = RegistrationLists.prepend(base, "c");

        assertThat(result).containsExactly("c", "a", "b");
        assertThat(base).containsExactly("a", "b");
    }

    @Test
    void prependReturnsImmutableList() {
        List<String> result = RegistrationLists.prepend(List.of("a"), "b");
        assertThatThrownBy(() -> result.add("z")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void prependAllKeepsLastHeadHighestPriority() {
        List<String> base = List.of("x");

        // as with successive prepend() calls: prepend(a) then prepend(b) -> [b, a, x]
        List<String> result = RegistrationLists.prependAll(base, List.of("a", "b"), Function.identity());

        assertThat(result).containsExactly("b", "a", "x");
        assertThat(base).containsExactly("x");
    }

    @Test
    void prependAllAppliesAdapter() {
        List<Integer> result = RegistrationLists.prependAll(List.of(0), List.of("a", "bb", "ccc"), String::length);
        assertThat(result).containsExactly(3, 2, 1, 0);
    }

    @Test
    void prependAllReturnsImmutableList() {
        List<String> result = RegistrationLists.prependAll(List.of("x"), List.of("a"), Function.identity());
        assertThatThrownBy(() -> result.add("z")).isInstanceOf(UnsupportedOperationException.class);
    }
}
