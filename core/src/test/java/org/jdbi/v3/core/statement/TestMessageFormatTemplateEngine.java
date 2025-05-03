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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestMessageFormatTemplateEngine {
    private TemplateEngine templateEngine;
    private StatementContext ctx;

    @BeforeEach
    @SuppressWarnings("deprecation")
    public void setUp() {
        templateEngine = new MessageFormatTemplateEngine();
        ctx = StatementContextAccess.createContext();
    }

    @Test
    public void testNoPlaceholdersNoValues() {
        assertThat(templateEngine.render("foo bar", ctx)).isEqualTo("foo bar");
    }

    @Test
    public void testWithPlaceholdersAndValues() {
        ctx.define("02", "!");
        ctx.define("000", "hello");
        ctx.define("01", "world");

        assertThat(templateEngine.render("{0} {1}{2}", ctx)).isEqualTo("hello world!");
    }

    @Test
    public void testManyValues() {
        ctx.define("000", "a");
        ctx.define("001", "b");
        ctx.define("002", "c");
        ctx.define("003", "d");
        ctx.define("004", "e");
        ctx.define("005", "f");
        ctx.define("006", "g");
        ctx.define("007", "h");
        ctx.define("008", "i");
        ctx.define("009", "j");
        ctx.define("010", "k");

        assertThat(templateEngine.render("{0}{1}{2}{3}{4}{5}{6}{7}{8}{9}{10}", ctx)).isEqualTo("abcdefghijk");
    }

    @Test
    public void testNoPlaceholdersButWithValues() {
        ctx.define("0", "hello");

        assertThatThrownBy(() -> templateEngine.render("foo bar", ctx))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("expected 0 keys but got 1");
    }

    @Test
    public void testWithPlaceholdersButNoValues() {
        assertThatThrownBy(() -> templateEngine.render("{0} bar", ctx))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("expected 1 keys but got 0");
    }

    @Test
    public void testNullValue() {
        ctx.define("0", null);

        assertThat(templateEngine.render("{0} bar", ctx))
            .isEqualTo("null bar");
    }

    @Test
    public void testNegativeKey() {
        ctx.define("-1", "hello");

        assertThatThrownBy(() -> templateEngine.render("{0} bar", ctx))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be 0");
    }

    @Test
    public void testDuplicateKey() {
        ctx.define("0", "hello");
        ctx.define("00", "world");

        assertThatThrownBy(() -> templateEngine.render("{0} {1}", ctx))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("0 was given more than once");
    }

    @Test
    public void testSkippedKey() {
        ctx.define("0", "hello");
        ctx.define("2", "world");

        assertThatThrownBy(() -> templateEngine.render("{0} {1}", ctx))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("skip from 0 to 2");
    }

    @Test
    public void testNonNumericKey() {
        ctx.define("abc", "hello");

        assertThatThrownBy(() -> templateEngine.render("{0} bar", ctx))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("\"abc\"");
    }

    @Test
    public void testWhitespaceInKey() {
        ctx.define(" 1 ", "hello");

        assertThatThrownBy(() -> templateEngine.render("{0} bar", ctx))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("\" 1 \"");
    }

    @Test
    public void testBlankKey() {
        ctx.define(" ", "hello");

        assertThatThrownBy(() -> templateEngine.render("{0} bar", ctx))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("\" \"");
    }

    @Test
    public void testEscaping() {
        ctx.define("0", "foo");

        assertThat(templateEngine.render("select * from {0} where name = ''john'' and stuff = '''{0}'''", ctx))
            .isEqualTo("select * from foo where name = 'john' and stuff = '{0}'");
    }
}
