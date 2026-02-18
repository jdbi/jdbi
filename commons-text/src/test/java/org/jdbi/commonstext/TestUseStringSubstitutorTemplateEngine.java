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
package org.jdbi.commonstext;

import org.jdbi.core.Jdbi;
import org.jdbi.core.statement.TemplateEngine;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.config.UseTemplateEngine;
import org.jdbi.sqlobject.customizer.Define;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestUseStringSubstitutorTemplateEngine {

    @RegisterExtension
    public JdbiExtension sqliteExtension = JdbiExtension.sqlite().withPlugin(new SqlObjectPlugin());

    private Jdbi jdbi;

    @BeforeEach
    public void before() {
        jdbi = sqliteExtension.getJdbi().setTemplateEngine(TemplateEngine.NOP);
    }

    @Test
    public void testUseTemplateEngine() {
        String selected = jdbi.withExtension(Queries1.class, q -> q.select("foo"));

        assertThat(selected).isEqualTo("foo");
    }

    @Test
    public void testCustomAnnotation() {
        String selected = jdbi.withExtension(Queries2.class, q -> q.select("foo"));

        assertThat(selected).isEqualTo("foo");
    }

    public interface Queries1 {
        @UseTemplateEngine(StringSubstitutorTemplateEngine.class)
        @SqlQuery("select * from (values('${v}'))")
        String select(@Define("v") String value);
    }

    public interface Queries2 {
        @UseStringSubstitutorTemplateEngine(prefix = "_", suffix = "_")
        @SqlQuery("select * from (values('_v_'))")
        String select(@Define("v") String value);
    }
}
