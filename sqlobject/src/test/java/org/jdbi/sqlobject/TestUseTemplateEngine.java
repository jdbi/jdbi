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
package org.jdbi.sqlobject;

import org.jdbi.core.Jdbi;
import org.jdbi.core.statement.DefinedAttributeTemplateEngine;
import org.jdbi.core.statement.MessageFormatTemplateEngine;
import org.jdbi.core.statement.TemplateEngine;
import org.jdbi.sqlobject.config.UseTemplateEngine;
import org.jdbi.sqlobject.customizer.Define;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestUseTemplateEngine {

    @RegisterExtension
    public JdbiExtension sqliteExtension = JdbiExtension.sqlite().withPlugin(new SqlObjectPlugin());

    private Jdbi jdbi;

    @BeforeEach
    public void before() {
        jdbi = sqliteExtension.getJdbi().setTemplateEngine(TemplateEngine.NOP);
    }

    @Test
    public void testUseMessageFormat() {
        String selected = jdbi.withExtension(QueriesForMessageFormatTE.class, q -> q.select("foo"));

        assertThat(selected).isEqualTo("foo");
    }

    @Test
    public void testUseDefinedAttributes() {
        String selected = jdbi.withExtension(QueriesForDefinedAttributeTE.class, q -> q.select("foo"));

        assertThat(selected).isEqualTo("foo");
    }

    @SuppressWarnings("deprecation")
    public interface QueriesForMessageFormatTE {
        @UseTemplateEngine(MessageFormatTemplateEngine.class)
        @SqlQuery("select * from (values(''{0}''))")
        String select(@Define("0") String value);
    }

    public interface QueriesForDefinedAttributeTE {
        @UseTemplateEngine(DefinedAttributeTemplateEngine.class)
        @SqlQuery("select * from (values(\\'<v>\\'))")
        String select(@Define("v") String value);
    }
}
