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

import org.jdbi.v3.core.junit5.DatabaseExtension;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class TestStatementExceptionContext {

    @RegisterExtension
    public DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @Test
    public void testFoo() {

        assertThatExceptionOfType(StatementException.class)
            .isThrownBy(() -> h2Extension.openHandle().execute("WOOF", 7, "Tom"))
            .satisfies(e -> assertThat(e.getStatementContext().getRawSql()).isEqualTo("WOOF"));
    }
}
