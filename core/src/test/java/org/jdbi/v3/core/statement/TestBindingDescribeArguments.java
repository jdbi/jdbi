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

import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.argument.MapArguments;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class TestBindingDescribeArguments {
    private final StatementContext ctx = StatementContextAccess.createContext();
    private final Binding binding = new Binding(ctx);

    private void parsedNamed(final String... names) {
        ctx.setParsedSql(ParsedSql.of("sql", ParsedParameters.named(List.of(names))));
    }

    private void parsedPositional(final int count) {
        ctx.setParsedSql(ParsedSql.of("sql", ParsedParameters.positional(count)));
    }

    @Test
    public void emptyBinding() {
        parsedNamed();

        assertThat(binding.describeArguments(512)).isEmpty();
    }

    @Test
    public void notParsed() {
        binding.addNamed("name", "Zebra");
        binding.addPositional(0, 1);

        assertThat(binding.describeArguments(512)).isEmpty();
    }

    @Test
    public void positionalParameters() {
        parsedPositional(2);
        binding.addPositional(1, 2);
        binding.addPositional(0, null);
        binding.addPositional(2, "unused");
        binding.addNamed("name", "Zebra");

        assertThat(binding.describeArguments(512))
            .containsExactly(
                entry("0", "null"),
                entry("1", "2"));
    }

    @Test
    public void namedParametersInSqlOrder() {
        parsedNamed("typed", "name", "typed");
        binding.addNamed("name", "Zebra");
        binding.addNamed("typed", 42L, QualifiedType.of(Long.class));
        binding.addNamed("unused", "Bananas");
        binding.addPositional(0, "Zebra");

        assertThat(binding.describeArguments(512))
            .containsExactly(
                entry("typed", "42"),
                entry("name", "Zebra"));
    }

    @Test
    public void namedArgumentFinder() {
        parsedNamed("name", "id", "missing");
        binding.addNamed("name", "Zebra");
        binding.addNamedArgumentFinder(new MapArguments(Map.of("name", "Bananas", "id", 1)));

        assertThat(binding.describeArguments(512))
            .containsExactly(
                entry("name", "Zebra"),
                entry("id", "1"));
    }

    @Test
    public void failingArgumentFinder() {
        parsedNamed("bad", "good");
        binding.addNamedArgumentFinder((name, c) -> {
            if ("bad".equals(name)) {
                throw new IllegalStateException();
            }
            return c.findArgumentFor(String.class, "Zebra");
        });

        assertThat(binding.describeArguments(512)).containsExactly(entry("good", "Zebra"));
    }

    @Test
    public void truncateEachValue() {
        parsedNamed("long", "short");
        binding.addNamed("long", "abcdefghij");
        binding.addNamed("short", "abc");

        assertThat(binding.describeArguments(5))
            .containsExactly(
                entry("long", "abcd…"),
                entry("short", "abc"));
    }

    @Test
    public void failingToString() {
        parsedNamed("bad", "good");
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
}
