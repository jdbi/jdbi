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
package org.jdbi.v3.stringsubstitutor;

import org.jdbi.v3.core.statement.ParsedSql;
import org.jdbi.v3.core.statement.StatementContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;

public class TestStringSubstitutorSqlParser {
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private StatementContext ctx;

    @Before
    public void before() {}

    @Test
    public void testBasics() {
        ParsedSql parsed = new StringSubstitutorSqlParser().parse("select * from foo where id = ${id}", ctx);

        assertThat(parsed.getSql()).isEqualTo("select * from foo where id = ?");
        assertThat(parsed.getParameters().getParameterCount()).isEqualTo(1);
        assertThat(parsed.getParameters().getParameterNames()).containsExactly("id");
    }
}
