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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.core.statement.StatementExceptions.limit;

public class TestStatementExceptions {
    @Test
    public final void testLimit() {
        String exception = "This is a very long exception that is used for testing";

        assertThat(limit(exception, 0)).isEqualTo("[...]");
        assertThat(limit(exception, 10)).isEqualTo("This is a [...]");
        assertThat(limit(exception, 20)).isEqualTo("This is a very long [...]");
        assertThat(limit(exception, 53)).isEqualTo("This is a very long exception that is used for testin[...]");
        assertThat(limit(exception, 54)).isEqualTo("This is a very long exception that is used for testing");
        assertThat(limit(exception, 99)).isEqualTo("This is a very long exception that is used for testing");
    }
}
