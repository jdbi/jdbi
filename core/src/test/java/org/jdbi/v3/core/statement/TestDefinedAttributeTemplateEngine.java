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

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestDefinedAttributeTemplateEngine {
    private TemplateEngine templateEngine;
    private StatementContext ctx;

    @Before
    public void setUp() {
        templateEngine = new DefinedAttributeTemplateEngine();
        ctx = mock(StatementContext.class);
    }

    private String render(String sql) {
        return render(sql, Collections.emptyMap());
    }

    private String render(String sql, Map<String, Object> attributes) {
        attributes.forEach((key, value) -> when(ctx.getAttribute(key)).thenReturn(value));

        return templateEngine.render(sql, ctx);
    }

    @Test
    public void testSubstitutesDefinedAttributes() {
        Map<String, Object> attributes = ImmutableMap.of(
                "column", "foo",
                "table", "bar");
        String rendered = render("select <column> from <table> where <column> = :someValue", attributes);
        assertThat(rendered).isEqualTo("select foo from bar where foo = :someValue");
    }

    @Test
    public void testUndefinedAttribute() {
        assertThatThrownBy(() -> render("select * from <table>", Collections.emptyMap()))
            .isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testNullAttribute() {
        assertThatThrownBy(() -> render("select * from something where id=<id>", Collections.singletonMap("id", null)))
            .isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testLeaveEnquotedTokensIntact() {
        String sql = "select '<foo>' foo, \"<bar>\" bar from something";
        assertThat(render(sql, ImmutableMap.of("foo", "no", "bar", "stahp"))).isEqualTo(sql);
    }

    @Test
    public void testIgnoreAngleBracketsNotPartOfToken() {
        String sql = "select * from foo where end_date < ? and start_date > ?";
        assertThat(render(sql)).isEqualTo(sql);
    }

    @Test
    public void testCommentQuote() {
        String sql = "select 1 /* ' \" <foo> */";
        assertThat(render(sql)).isEqualTo(sql);
    }

    @Test
    public void testColonInComment() {
        String sql = "/* comment with : colons :: inside it */ select 1";
        assertThat(render(sql)).isEqualTo(sql);
    }
}
