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
package org.skife.jdbi.v2.sqlobject.stringtemplate;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.AngleBracketTemplateLexer;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.StatementLocator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;

public class StringTemplate3StatementLocator implements StatementLocator
{
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final ConcurrentMap<String, StringTemplateGroup> ANNOTATION_LOCATOR_CACHE;
    private static final String SUPER_SEPARATOR = " > ";

    public static final String TEMPLATE_GROUP_EXTENSION = ".sql.stg";

    static {
        final ConcurrentMap<String, StringTemplateGroup> cache = new ConcurrentHashMap<String, StringTemplateGroup>();
        cache.put("empty template group", new StringTemplateGroup("empty template group", AngleBracketTemplateLexer.class));
        ANNOTATION_LOCATOR_CACHE = cache;
    }

    private final StringTemplateGroup literals = new StringTemplateGroup("literals", AngleBracketTemplateLexer.class);
    private final StringTemplateGroup group;
    private boolean treatLiteralsAsTemplates;

    public StringTemplate3StatementLocator(Class baseClass)
    {
        this(mungify(baseClass), false, false);
    }

    public StringTemplate3StatementLocator(Class baseClass,
                                           boolean allowImplicitTemplateGroup,
                                           boolean treatLiteralsAsTemplates)
    {
        this(mungify(baseClass),
             null,
             allowImplicitTemplateGroup,
             treatLiteralsAsTemplates,
             false);
    }

    public StringTemplate3StatementLocator(String templateGroupFilePathOnClasspath)
    {
        this(templateGroupFilePathOnClasspath,
            null,
            false,
            false,
            false);
    }

    public StringTemplate3StatementLocator(String templateGroupFilePathOnClasspath,
                                           boolean allowImplicitTemplateGroup,
                                           boolean treatLiteralsAsTemplates)
    {
        this(templateGroupFilePathOnClasspath,
            null,
            allowImplicitTemplateGroup,
            treatLiteralsAsTemplates,
            false);
    }

    public StringTemplate3StatementLocator(Class baseClass,
                                           boolean allowImplicitTemplateGroup,
                                           boolean treatLiteralsAsTemplates,
                                           boolean shouldCache)
    {
        this(mungify(baseClass),
            null,
            allowImplicitTemplateGroup,
            treatLiteralsAsTemplates,
            shouldCache);
    }

    public StringTemplate3StatementLocator(String templateGroupFilePathOnClasspath,
                                           boolean allowImplicitTemplateGroup,
                                           boolean treatLiteralsAsTemplates,
                                           boolean shouldCache)
    {
        this(templateGroupFilePathOnClasspath,
            null,
            allowImplicitTemplateGroup,
            treatLiteralsAsTemplates,
            shouldCache);
    }


    public StringTemplate3StatementLocator(Class<?> baseClass,
                                           Class<?> superTemplateGroupClass,
                                           boolean allowImplicitTemplateGroup,
                                           boolean treatLiteralsAsTemplates,
                                           boolean shouldCache)
    {
        this(mungify(baseClass),
            mungify(superTemplateGroupClass),
            allowImplicitTemplateGroup,
            treatLiteralsAsTemplates,
            shouldCache);
    }

    public StringTemplate3StatementLocator(String templateGroupFilePathOnClasspath,
                                           String superTemplateGroupFilePathOnClasspath,
                                           boolean allowImplicitTemplateGroup,
                                           boolean treatLiteralsAsTemplates,
                                           boolean shouldCache)
    {
        this.treatLiteralsAsTemplates = treatLiteralsAsTemplates;

        final StringTemplateGroup superGroup;

        final StringBuilder sb = new StringBuilder(templateGroupFilePathOnClasspath);

        if (superTemplateGroupFilePathOnClasspath != null) {
            sb.append(SUPER_SEPARATOR).append(superTemplateGroupFilePathOnClasspath);

            superGroup = createGroup(superTemplateGroupFilePathOnClasspath,
                                     shouldCache ? superTemplateGroupFilePathOnClasspath : null,
                                     allowImplicitTemplateGroup,
                                     getClass(),
                                     null);
        }
        else {
            superGroup = null;
        }

        this.group = createGroup(templateGroupFilePathOnClasspath,
            shouldCache ? sb.toString() : null,
            allowImplicitTemplateGroup,
            getClass(),
            superGroup);
    }


    private static StringTemplateGroup createGroup(final String templateGroupFilePathOnClasspath, final String cacheKey, final boolean allowImplicitTemplateGroup, final Class<?> clazz, final StringTemplateGroup superGroup)
    {
        if (cacheKey != null && ANNOTATION_LOCATOR_CACHE.containsKey(cacheKey)) {
            return ANNOTATION_LOCATOR_CACHE.get(cacheKey);
        }

        InputStream ins = clazz.getResourceAsStream(templateGroupFilePathOnClasspath);

        if (allowImplicitTemplateGroup && ins == null) {
            return ANNOTATION_LOCATOR_CACHE.get("empty template group");
        }
        else if (ins == null) {
            throw new IllegalStateException("unable to find group file "
                                            + templateGroupFilePathOnClasspath
                                            + " on classpath");
        }
        else {
            InputStreamReader reader = new InputStreamReader(ins, UTF_8);
            StringTemplateGroup result;

            if (superGroup == null) {
                result = new StringTemplateGroup(reader, AngleBracketTemplateLexer.class);
            }
            else {
                result = new StringTemplateGroup(reader, AngleBracketTemplateLexer.class, null, superGroup);
            }

            if (cacheKey != null) {
                StringTemplateGroup oldGroup = ANNOTATION_LOCATOR_CACHE.putIfAbsent(cacheKey, result);
                if (oldGroup != null) {
                    result = oldGroup;
                }
            }

            try {
                reader.close();
            }
            catch (IOException e) {
                throw new IllegalStateException("unable to load string template group " + templateGroupFilePathOnClasspath, e);
            }

            return result;
        }
    }

    public String locate(String name, StatementContext ctx) throws Exception
    {
        if (group.isDefined(name)) {
            // yeah, found template for it!
            StringTemplate t = group.getInstanceOf(name);
            t.reset();
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

    private static final String sep = "/"; // *Not* System.getProperty("file.separator"), which breaks in jars

    private static String mungify(Class<?> clazz)
    {
        return "/" + clazz.getName().replaceAll("\\.", Matcher.quoteReplacement(sep)) + TEMPLATE_GROUP_EXTENSION;
    }

    // @VisibleForTesting
    static boolean templateCached(final Class<?> clazzKey, Class<?> superKey)
    {
        final StringBuilder sb = new StringBuilder(mungify(clazzKey));
        if (superKey != null) {
            sb.append(SUPER_SEPARATOR);
            sb.append(mungify(superKey));
        }
        return ANNOTATION_LOCATOR_CACHE.containsKey(sb.toString());
    }
}
