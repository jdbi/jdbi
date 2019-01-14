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

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestParsedSql {

    @Test
    public void testFactoryNamedParameters() {

        final List<String> names = Arrays.asList("a", "b", "c");

        final String sql = "insert into test (a, b, c) values (?, ?, ?)";
        final ParsedParameters parameters = ParsedParameters.named(names);
        final ParsedSql parsedSql = ParsedSql.of(sql, parameters);

        assertThat(parsedSql).isNotNull();
        assertThat(parsedSql.getSql()).isEqualTo(sql);
        assertThat(parsedSql.getParameters().isPositional()).isFalse();
        assertThat(parsedSql.getParameters().getParameterNames()).containsExactly("a", "b", "c");
    }

    @Test
    public void testFactoryPositionalParameters() {

        final String sql = "insert into test (a, b, c) values (?, ?, ?)";
        final ParsedParameters parameters = ParsedParameters.positional(3);
        final ParsedSql parsedSql = ParsedSql.of(sql, parameters);

        assertThat(parsedSql).isNotNull();
        assertThat(parsedSql.getSql()).isEqualTo(sql);
        assertThat(parsedSql.getParameters().isPositional()).isTrue();
        assertThat(parsedSql.getParameters().getParameterNames()).containsOnly("?");
    }

    @Test
    public void testBuilderWithNamedParameters() {

        final List<String> names = Arrays.asList("a", "b", "c");

        final ParsedSql parsedSql = ParsedSql.builder()
            .append("insert into test (a, b, c) values (")
            .appendNamedParameter(names.get(0))
            .append(", ")
            .appendNamedParameter(names.get(1))
            .append(", ")
            .appendNamedParameter(names.get(2))
            .append(")")
            .build();

        assertThat(parsedSql).isNotNull();
        assertThat(parsedSql.getSql()).isEqualTo("insert into test (a, b, c) values (?, ?, ?)");
        assertThat(parsedSql.getParameters().isPositional()).isFalse();
        assertThat(parsedSql.getParameters().getParameterNames()).containsExactly("a", "b", "c");
    }

    @Test
    public void testBuilderWithPositionalParameters() {

        final ParsedSql parsedSql = ParsedSql.builder()
            .append("insert into test (a, b, c) values (")
            .appendPositionalParameter()
            .append(", ")
            .appendPositionalParameter()
            .append(", ")
            .appendPositionalParameter()
            .append(")")
            .build();

        assertThat(parsedSql).isNotNull();
        assertThat(parsedSql.getSql()).isEqualTo("insert into test (a, b, c) values (?, ?, ?)");
        assertThat(parsedSql.getParameters().isPositional()).isTrue();
        assertThat(parsedSql.getParameters().getParameterNames()).containsOnly("?");
    }
}
