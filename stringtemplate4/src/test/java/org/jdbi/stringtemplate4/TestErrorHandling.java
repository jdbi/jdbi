package org.jdbi.stringtemplate4;
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

import org.jdbi.core.Handle;
import org.jdbi.core.statement.UnableToCreateStatementException;
import org.jdbi.core.statement.UnableToExecuteStatementException;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestErrorHandling {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2();

    Handle handle;

    @BeforeEach
    void setup() {
        handle = h2Extension.getSharedHandle();
        handle.setTemplateEngine(new StringTemplateEngine());
    }

    @Test
    void testSyntaxError() {
        assertThatThrownBy(() ->
            h2Extension.getSharedHandle().createQuery("select <a")
                .mapTo(String.class)
                .one())
            .isInstanceOf(UnableToCreateStatementException.class)
            .hasMessageContaining("Compiling StringTemplate failed", "premature EOF");
    }

    @Test
    void testUnboundVar() {
        assertThat(
            h2Extension.getSharedHandle().createQuery("select <if(a)> true <else> false <endif>")
                .mapTo(boolean.class)
                .one())
            .isEqualTo(false);
    }

    @Test
    void testUnboundVarThrows() {
        assertThatThrownBy(() ->
            h2Extension.getSharedHandle().createQuery("select <if(a)> true <else> false <endif>")
                    .configure(StringTemplates.class, st -> st.setFailOnMissingAttribute(true))
                .mapTo(boolean.class)
                .one())
            .isInstanceOf(UnableToExecuteStatementException.class)
            .hasMessageContaining("Executing StringTemplate failed", "attribute a isn't defined");
    }
}
