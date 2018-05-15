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

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestMessageFormatTemplateEngine {
    private TemplateEngine templateEngine;
    private StatementContext ctx;
    private Map<String, Object> attributes;

    @Before
    public void setUp() {
        templateEngine = MessageFormatTemplateEngine.INSTANCE;
        attributes = new HashMap<>();
        ctx = mock(StatementContext.class);
        when(ctx.getAttributes()).thenReturn(attributes);
    }

    @Test
    public void testNoPlaceholdersNoValues() {
        attributes.clear();

        assertThat(templateEngine.render("foo bar", ctx)).isEqualTo("foo bar");
    }

    @Test
    public void testWithPlaceholdersAndValues() {
        attributes.put("02", "!");
        attributes.put("000", "hello");
        attributes.put("01", "world");

        assertThat(templateEngine.render("{0} {1}{2}", ctx)).isEqualTo("hello world!");
    }

    @Test
    public void testManyValues() {
        attributes.put("000", "a");
        attributes.put("001", "b");
        attributes.put("002", "c");
        attributes.put("003", "d");
        attributes.put("004", "e");
        attributes.put("005", "f");
        attributes.put("006", "g");
        attributes.put("007", "h");
        attributes.put("008", "i");
        attributes.put("009", "j");
        attributes.put("010", "k");

        assertThat(templateEngine.render("{0}{1}{2}{3}{4}{5}{6}{7}{8}{9}{10}", ctx)).isEqualTo("abcdefghijk");
    }

    @Test
    public void testNoPlaceholdersButWithValues() {
        attributes.put("0", "hello");

        assertThatThrownBy(() -> templateEngine.render("foo bar", ctx))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("expected 0 keys but got 1");
    }

    @Test
    public void testWithPlaceholdersButNoValues() {
        attributes.clear();

        assertThatThrownBy(() -> templateEngine.render("{0} bar", ctx))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("expected 1 keys but got 0");
    }

    @Test
    public void testNegativeKey() {
        attributes.put("-1", "hello");

        assertThatThrownBy(() -> templateEngine.render("{0} bar", ctx))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be 0");
    }

    @Test
    public void testDuplicateKey() {
        attributes.put("0", "hello");
        attributes.put("00", "world");

        assertThatThrownBy(() -> templateEngine.render("{0} {1}", ctx))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("0 was given more than once");
    }

    @Test
    public void testSkippedKey() {
        attributes.put("0", "hello");
        attributes.put("2", "world");

        assertThatThrownBy(() -> templateEngine.render("{0} {1}", ctx))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("skip from 0 to 2");
    }

    @Test
    public void testNonNumericKey() {
        attributes.put("abc", "hello");

        assertThatThrownBy(() -> templateEngine.render("{0} bar", ctx))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("\"abc\"");
    }

    @Test
    public void testWhitespaceInKey() {
        attributes.put(" 1 ", "hello");

        assertThatThrownBy(() -> templateEngine.render("{0} bar", ctx))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("\" 1 \"");
    }

    @Test
    public void testBlankKey() {
        attributes.put(" ", "hello");

        assertThatThrownBy(() -> templateEngine.render("{0} bar", ctx))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("\" \"");
    }

    @Test
    public void testEscaping() {
        attributes.put("0", "foo");

        assertThat(templateEngine.render("select * from {0} where name = ''john'' and stuff = '''{0}'''", ctx))
            .isEqualTo("select * from foo where name = 'john' and stuff = '{0}'");
    }
}
