/* Copyright 2004-2007 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2.unstable.stringtemplate;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.StringTemplateGroupLoader;
import org.skife.jdbi.v2.ClasspathStatementLocator;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.StatementLocator;

import java.util.StringTokenizer;

/**
 * StatementLocator which uses <a href="http://stringtemplate.org/">StringTemplate</a>
 * to build statements.
 * <p>
 */
public class StringTemplateStatementLocator implements StatementLocator
{
    private final StringTemplateGroupLoader loader;

    public StringTemplateStatementLocator(StringTemplateGroupLoader loader) {
        this.loader = loader;
    }

    /**
     * Use this to map from a named statement to SQL. The SQL returned will be passed to
     * a StatementRewriter, so this can include stuff like named params and whatnot.
     *
     * @param name The name of the statement, as provided to a Handle
     *
     * @return the SQL to execute, after it goes through a StatementRewriter
     *
     * @throws Exception if anything goes wrong, jDBI will percolate expected exceptions
     */
    public String locate(String name, StatementContext ctx) throws Exception
    {
        if (ClasspathStatementLocator.looksLikeSql(name)) {
            return name;
        }
        final StringTokenizer tok = new StringTokenizer(name, ":");
        final String group_name = tok.nextToken();
        final String template_name = tok.nextToken();
        final StringTemplateGroup group = loader.loadGroup(group_name);
        final StringTemplate template = group.getInstanceOf(template_name);

        template.setAttributes(ctx.getAttributes());
        return template.toString();
    }
}
