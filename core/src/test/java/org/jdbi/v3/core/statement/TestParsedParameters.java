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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestParsedParameters {

    @Test
    public void testFactoryNamedParameters() {

        final List<String> names = Arrays.asList("a", "b", "c");
        final ParsedParameters parameters = ParsedParameters.named(names);

        assertThat(parameters).isNotNull();
        assertThat(parameters.isPositional()).isFalse();
        assertThat(parameters.getParameterCount()).isEqualTo(3);
        assertThat(parameters.getParameterNames()).containsExactly("a", "b", "c");
    }

    @Test
    public void testFactoryNamedAndPositionalParametersMix() {
        assertThatThrownBy(() -> ParsedParameters.named(Arrays.asList("a", "b", "?")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Named parameters list must not contain positional parameter \"?\"");
    }

    @Test
    public void testFactoryPositionalParameters() {

        final ParsedParameters parameters = ParsedParameters.positional(3);

        assertThat(parameters).isNotNull();
        assertThat(parameters.isPositional()).isTrue();
        assertThat(parameters.getParameterCount()).isEqualTo(3);
        assertThat(parameters.getParameterNames()).containsOnly("?");
    }
}
