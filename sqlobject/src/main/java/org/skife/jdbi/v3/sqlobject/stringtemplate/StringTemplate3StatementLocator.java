/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
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
package org.skife.jdbi.v3.sqlobject.stringtemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.regex.Matcher;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.AngleBracketTemplateLexer;
import org.skife.jdbi.v3.StatementContext;
import org.skife.jdbi.v3.tweak.StatementLocator;

public class StringTemplate3StatementLocator implements StatementLocator
{
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final StringTemplateGroup group;
    private final StringTemplateGroup literals = new StringTemplateGroup("literals", AngleBracketTemplateLexer.class);

    private boolean treatLiteralsAsTemplates;

    public StringTemplate3StatementLocator(Class baseClass)
    {
        this(mungify("/" + baseClass.getName()) + ".sql.stg", false, false);
    }

    public StringTemplate3StatementLocator(Class baseClass,
                                           boolean allowImplicitTemplateGroup,
                                           boolean treatLiteralsAsTemplates)
    {
        this(mungify("/" + baseClass.getName()) + ".sql.stg", allowImplicitTemplateGroup, treatLiteralsAsTemplates);
    }

    public StringTemplate3StatementLocator(String templateGroupFilePathOnClasspath)
    {
        this(templateGroupFilePathOnClasspath, false, false);
    }

    public StringTemplate3StatementLocator(String templateGroupFilePathOnClasspath,
                                           boolean allowImplicitTemplateGroup,
                                           boolean treatLiteralsAsTemplates)
    {
        this.treatLiteralsAsTemplates = treatLiteralsAsTemplates;
        InputStream ins = getClass().getResourceAsStream(templateGroupFilePathOnClasspath);
        if (allowImplicitTemplateGroup && ins == null) {
            this.group = new StringTemplateGroup("empty template group", AngleBracketTemplateLexer.class);
        }
        else if (ins == null) {
            throw new IllegalStateException("unable to find group file "
                                            + templateGroupFilePathOnClasspath
                                            + " on classpath");
        }
        else {
            InputStreamReader reader = new InputStreamReader(ins, UTF_8);
            try {
                this.group = new StringTemplateGroup(reader, AngleBracketTemplateLexer.class);
                reader.close();
            }
            catch (IOException e) {
                throw new IllegalStateException("unable to load string template group " + templateGroupFilePathOnClasspath,
                                                e);
            }
        }
    }

    public String locate(String name, StatementContext ctx) throws Exception
    {
        if (group.isDefined(name)) {
            // yeah, found template for it!
            StringTemplate t = group.lookupTemplate(name);
            for (Map.Entry<String, Object> entry : ctx.getAttributes().entrySet()) {
                t.setAttribute(entry.getKey(), entry.getValue());
            }
            return t.toString();
        }
        else if (treatLiteralsAsTemplates) {
            // no template in the template group, but we want literals to be templates
            final String key = new String(new Base64().encode(name.getBytes(UTF_8)), UTF_8);
            if (!literals.isDefined(key)) {
                literals.defineTemplate(key, name);
            }
            StringTemplate t = literals.lookupTemplate(key);
            for (Map.Entry<String, Object> entry : ctx.getAttributes().entrySet()) {
                t.setAttribute(entry.getKey(), entry.getValue());
            }
            return t.toString();
        }
        else {
            // no template, no literals as template, just use the literal as sql
            return name;
        }
    }

    private final static String sep = "/"; // *Not* System.getProperty("file.separator"), which breaks in jars

    private static String mungify(String path)
    {
        return path.replaceAll("\\.", Matcher.quoteReplacement(sep));
    }

}
