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
package org.jdbi.v3.stringtemplate4;

import org.jdbi.v3.core.statement.TemplateEngine;
import org.jdbi.v3.core.statement.StatementContext;
import org.stringtemplate.v4.ST;

/**
 * Rewrites a StringTemplate template, using the attributes on the {@link StatementContext} as template parameters.
 */
public class StringTemplateEngine implements TemplateEngine {
    @Override
    public String render(String sql, StatementContext ctx) {
        ST template = new ST(sql);

        ctx.getAttributes().forEach(template::add);

        return template.render();
    }
}
