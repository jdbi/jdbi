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
package org.jdbi.v3.commonstext;

import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementContextAccess;
import org.jdbi.v3.core.statement.TemplateEngine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestStringSubstitutorTemplateEngine {

    private StatementContext ctx = StatementContextAccess.createContext();

    @Test
    public void testDefaults() {
        ctx.define("name", "foo");

        assertThat(new StringSubstitutorTemplateEngine().render("create table ${name};", ctx))
            .isEqualTo("create table foo;");
    }

    @Test
    public void testMissingAttribute() {
        TemplateEngine engine = StringSubstitutorTemplateEngine.between('<', '>');

        assertThat(engine.render("select * from foo where x=<x>", ctx))
            .isEqualTo("select * from foo where x=<x>");
    }

    @Test
    public void testNullAttribute() {
        ctx.define("x", null);

        TemplateEngine engine = StringSubstitutorTemplateEngine.between('<', '>');

        assertThat(engine.render("select * from foo where x=<x>", ctx))
            .isEqualTo("select * from foo where x=<x>");
    }

    @Test
    public void testCustomPrefixSuffix() {
        ctx.define("name", "foo");

        TemplateEngine engine = StringSubstitutorTemplateEngine.between('<', '>');

        assertThat(engine.render("create table <name>;", ctx))
            .isEqualTo("create table foo;");
    }

    @Test
    public void testEscapeCharacter() {
        ctx.define("name", "foo");

        TemplateEngine engine = StringSubstitutorTemplateEngine.between('<', '>', '@');

        assertThat(engine.render("create table @<name>;", ctx))
            .isEqualTo("create table <name>;");
    }
}
