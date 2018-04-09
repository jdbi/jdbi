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

public class TestMessageFormatTemplateEngine
{
    private TemplateEngine templateEngine;
    private StatementContext ctx;
    private Map<String, Object> attributes;

    @Before
    public void setUp()
    {
        templateEngine = MessageFormatTemplateEngine.INSTANCE;
        attributes = new HashMap<>();
        ctx = mock(StatementContext.class);
        when(ctx.getAttributes()).thenReturn(attributes);
    }

    @Test
    public void testNoPlaceholdersNoValues()
    {
        attributes.clear();

        assertThat(templateEngine.render("foo bar", ctx)).isEqualTo("foo bar");
    }

    @Test
    public void testNoPlaceholdersButWithValues()
    {
        attributes.put("0", "hello");

        templateEngine.render("foo bar", ctx);
    }

    @Test
    public void testWithPlaceholdersButNoValues()
    {
        attributes.clear();

        templateEngine.render("{0} bar", ctx);
    }

    @Test
    public void testWithPlaceholdersAndValues()
    {
        attributes.put("00", "hello");
        attributes.put("01", "world");
        attributes.put("02", "!");

        assertThat(templateEngine.render("{0} {1}{2}", ctx)).isEqualTo("hello world!");
    }

    @Test
    public void testNegativeKey()
    {
        attributes.put("-1", "hello");

        assertThatThrownBy(() -> templateEngine.render("{0} bar", ctx)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testDuplicateKey()
    {
        attributes.put("0", "hello");
        attributes.put("00", "world");

        assertThatThrownBy(() -> templateEngine.render("{0} bar", ctx)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testSkippedKey()
    {
        attributes.put("0", "hello");
        attributes.put("2", "world");

        assertThatThrownBy(() -> templateEngine.render("{0} bar", ctx)).isInstanceOf(IllegalArgumentException.class);
    }
}
