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

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.StatementException;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class TestStatementExceptionContext {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Test
    public void testFoo() throws Exception {

        assertThatExceptionOfType(StatementException.class)
                .isThrownBy(()-> db.openHandle().insert("WOOF", 7, "Tom"))
                .satisfies(e -> assertThat(e.getStatementContext().getRawSql()).isEqualTo("WOOF"));
    }
}
