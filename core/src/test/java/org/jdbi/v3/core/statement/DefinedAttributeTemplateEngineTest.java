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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class DefinedAttributeTemplateEngineTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance()
            .withConfig(SqlStatements.class, st -> st.setTemplateEngine(new DefinedAttributeTemplateEngine()));

    String renderedSql;

    @Test
    public void testBackslash1906() {
        final Handle handle = h2Extension.getSharedHandle();
        final SqlLogger captureSql = new SqlLogger() {
            @Override
            public void logAfterExecution(final StatementContext context) {
                renderedSql = context.getRenderedSql();
            }
        };
        handle.setSqlLogger(captureSql);
        final String backslashSql = "SELECT '\\\\' = '\\\\'";
        handle
                .createQuery(backslashSql)
                .mapTo(boolean.class)
                .one();
        assertThat(renderedSql).isEqualTo(backslashSql);
    }
}
