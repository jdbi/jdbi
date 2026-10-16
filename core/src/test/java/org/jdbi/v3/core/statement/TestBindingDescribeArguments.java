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
package org.jdbi.v3.core.statement;

import java.util.Map;

import org.jdbi.v3.core.argument.MapArguments;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class TestBindingDescribeArguments {
    private final StatementContext ctx = StatementContextAccess.createContext();
    private final Binding binding = new Binding(ctx);

    @Test
    public void emptyBinding() {
        assertThat(binding.describeArguments(512)).isEmpty();
    }

    @Test
    public void positionalBeforeNamed() {
        binding.addNamed("name", "Zebra");
        binding.addPositional(1, 2);
        binding.addPositional(0, null);
        binding.addNamed("typed", 42L, QualifiedType.of(Long.class));

        assertThat(binding.describeArguments(512))
            .containsExactly(
                entry("0", "null"),
                entry("1", "2"),
                entry("name", "Zebra"),
                entry("typed", "42"));
    }

    @Test
    public void truncateEachValue() {
        binding.addNamed("long", "abcdefghij");
        binding.addNamed("short", "abc");

        assertThat(binding.describeArguments(5))
            .containsExactly(
                entry("long", "abcd…"),
                entry("short", "abc"));
    }

    @Test
    public void failingToString() {
        final Object unprintable = new Object() {
            @Override
            public String toString() {
                throw new IllegalStateException();
            }
        };
        binding.addNamed("bad", unprintable);
        binding.addNamed("good", "Zebra");
        final String description = "<" + unprintable.getClass().getName() + ".toString() threw java.lang.IllegalStateException>";

        assertThat(binding.describeArguments(512))
            .containsExactly(
                entry("bad", description),
                entry("good", "Zebra"));
        assertThat(binding.describe(512)).isEqualTo("{named:{bad:" + description + ",good:Zebra}}");
    }

    @Test
    public void namedArgumentFinderNotIncluded() {
        binding.addNamedArgumentFinder(new MapArguments(Map.of("secret", "value")));

        assertThat(binding.describeArguments(512)).isEmpty();
    }
}
