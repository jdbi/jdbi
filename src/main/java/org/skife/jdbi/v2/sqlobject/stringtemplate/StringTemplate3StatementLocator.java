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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.AngleBracketTemplateLexer;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.StatementLocator;

public class StringTemplate3StatementLocator implements StatementLocator {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final StringTemplateGroup group;
    private final StringTemplateGroup literals = new StringTemplateGroup("literals", AngleBracketTemplateLexer.class);

    protected final ConcurrentHashMap<LocatorKey, String> cachedEvaluatedTemplate = new ConcurrentHashMap<LocatorKey, String>();
    private boolean shouldCacheEvaluatedTemplate = false;
    private boolean treatLiteralsAsTemplates;


    public StringTemplate3StatementLocator(Class baseClass) {
        this(mungify("/" + baseClass.getName()) + ".sql.stg", false, false);
    }

    public StringTemplate3StatementLocator(Class baseClass,
                                           boolean allowImplicitTemplateGroup,
                                           boolean treatLiteralsAsTemplates) {
        this(mungify("/" + baseClass.getName()) + ".sql.stg", allowImplicitTemplateGroup, treatLiteralsAsTemplates);
    }

    public StringTemplate3StatementLocator(String templateGroupFilePathOnClasspath) {
        this(templateGroupFilePathOnClasspath, false, false);
    }

    public StringTemplate3StatementLocator(String templateGroupFilePathOnClasspath,
                                           boolean allowImplicitTemplateGroup,
                                           boolean treatLiteralsAsTemplates) {
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
            } catch (IOException e) {
                throw new IllegalStateException("unable to load string template group " + templateGroupFilePathOnClasspath,
                                                e);
            }
        }
    }


    public StringTemplate3StatementLocator(Class baseClass,
                                           boolean allowImplicitTemplateGroup,
                                           boolean treatLiteralsAsTemplates,
                                           Class superTemplateGroupClass) {
        this(mungify("/" + baseClass.getName()) + ".sql.stg", allowImplicitTemplateGroup, treatLiteralsAsTemplates, superTemplateGroupClass);
    }

    public StringTemplate3StatementLocator(String templateGroupFilePathOnClasspath,
                                           boolean allowImplicitTemplateGroup,
                                           boolean treatLiteralsAsTemplates,
                                           Class superTemplateGroupClass) {
        this.treatLiteralsAsTemplates = treatLiteralsAsTemplates;

        final String superTemplateGroupFilePathOnClasspath = (superTemplateGroupClass != null) ? mungify("/" + superTemplateGroupClass.getName()) + ".sql.stg" : null;
        final StringTemplateGroup superGroup = (superTemplateGroupFilePathOnClasspath != null) ?
                                               createGroup(superTemplateGroupFilePathOnClasspath, allowImplicitTemplateGroup, getClass(), null) :
                                               null;

        this.group = createGroup(templateGroupFilePathOnClasspath, allowImplicitTemplateGroup, getClass(), superGroup);
    }

    public void setCachedEvaluatedTemplate() {
        this.shouldCacheEvaluatedTemplate = true;
    }


    private static StringTemplateGroup createGroup(final String templateGroupFilePathOnClasspath, final boolean allowImplicitTemplateGroup, final Class claz, final StringTemplateGroup superGroup) {


        StringTemplateGroup result = null;

        InputStream ins = claz.getResourceAsStream(templateGroupFilePathOnClasspath);

        if (allowImplicitTemplateGroup && ins == null) {
            result = new StringTemplateGroup("empty template group", AngleBracketTemplateLexer.class);
        }
        else if (ins == null) {
            throw new IllegalStateException("unable to find group file "
                                            + templateGroupFilePathOnClasspath
                                            + " on classpath");
        }
        else {
            InputStreamReader reader = new InputStreamReader(ins, UTF_8);
            try {
                result = superGroup != null ? new StringTemplateGroup(reader, AngleBracketTemplateLexer.class, null, superGroup)
                                            : new StringTemplateGroup(reader, AngleBracketTemplateLexer.class);
                reader.close();
            } catch (IOException e) {
                throw new IllegalStateException("unable to load string template group " + templateGroupFilePathOnClasspath,
                                                e);
            }
        }
        return result;
    }

    public String locate(String name, StatementContext ctx) throws Exception {

        final LocatorKey locatorKey = new LocatorKey(name, ctx.getAttributes());
        // First check for local cache to see if StringTemplate was already evaluated
        if (shouldCacheEvaluatedTemplate && cachedEvaluatedTemplate.containsKey(locatorKey)) {
            return cachedEvaluatedTemplate.get(locatorKey);
        }
        else if (group.isDefined(name)) {
            // yeah, found template for it!
            StringTemplate t = group.lookupTemplate(name);
            return evaluateTemplate(locatorKey, t);
        }
        else if (treatLiteralsAsTemplates) {
            // no template in the template group, but we want literals to be templates
            final String key = new String(new Base64().encode(name.getBytes(UTF_8)), UTF_8);
            if (!literals.isDefined(key)) {
                literals.defineTemplate(key, name);
            }
            StringTemplate t = literals.lookupTemplate(key);
            return evaluateTemplate(locatorKey, t);
        }
        else {
            // no template, no literals as template, just use the literal as sql
            return name;
        }
    }

    private String evaluateTemplate(final LocatorKey locatorKey, final StringTemplate t) {
        // Perform a copy of the string template -- on which the attributes mao is empty before we start applying the attributes
        final StringTemplate dup = t.getInstanceOf();
        for (Map.Entry<String, Object> entry : locatorKey.getAttributes().entrySet()) {
            dup.setAttribute(entry.getKey(), entry.getValue());
        }
        // Perform evaluation
        final String result = dup.toString();
        // Add to cache if neeeded
        if (shouldCacheEvaluatedTemplate) {
            cachedEvaluatedTemplate.putIfAbsent(locatorKey, result);
        }
        return result;
    }

    // The key associated with a given instance of a StringTemplate evaluation.
    // Same name with different attributes will yield to different keys
    protected static class LocatorKey {

        private final String name;
        private final Map<String, Object> attributes;

        public LocatorKey(final String name, final Map<String, Object> attributes) {
            this.name = name;
            this.attributes = attributes;
        }

        public String getName() {
            return name;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof LocatorKey)) {
                return false;
            }

            final LocatorKey that = (LocatorKey) o;

            if (!attributes.equals(that.attributes)) {
                return false;
            }
            if (!name.equals(that.name)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + attributes.hashCode();
            return result;
        }
    }


    private final static String sep = "/"; // *Not* System.getProperty("file.separator"), which breaks in jars

    private static String mungify(String path)
    {
        return path.replaceAll("\\.", Matcher.quoteReplacement(sep));
    }

}
