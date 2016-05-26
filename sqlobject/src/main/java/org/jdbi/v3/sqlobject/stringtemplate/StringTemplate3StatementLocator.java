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
package org.jdbi.v3.sqlobject.stringtemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateErrorListener;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.jdbi.v3.StatementContext;
import org.jdbi.v3.tweak.StatementLocator;

public class StringTemplate3StatementLocator implements StatementLocator
{
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final ConcurrentMap<String, StringTemplateGroup> ANNOTATION_LOCATOR_CACHE;
    private static final String SUPER_SEPARATOR = " > ";

    public static final String TEMPLATE_GROUP_EXTENSION = ".sql.stg";

    static {
        final ConcurrentMap<String, StringTemplateGroup> cache = new ConcurrentHashMap<>();
        cache.put("empty template group", new StringTemplateGroup("empty template group"));
        ANNOTATION_LOCATOR_CACHE = cache;
    }

    private final StringTemplateGroup literals = new StringTemplateGroup("literals");
    private final StringTemplateGroup group;
    private final boolean treatLiteralsAsTemplates;

    public static StringTemplate3StatementLocator.Builder builder(Class<?> baseClass)
    {
        return new Builder(baseClass);
    }

    public static StringTemplate3StatementLocator.Builder builder(String path)
    {
        return new Builder(path);
    }

    private StringTemplate3StatementLocator(String templateGroupFilePathOnClasspath,
                                            String superTemplateGroupFilePathOnClasspath,
                                            StringTemplateErrorListener errorListener,
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
                errorListener,
                shouldCache ? superTemplateGroupFilePathOnClasspath : null,
                allowImplicitTemplateGroup,
                getClass(),
                null);
        }
        else {
            superGroup = null;
        }

        this.group = createGroup(templateGroupFilePathOnClasspath,
            errorListener,
            shouldCache ? sb.toString() : null,
            allowImplicitTemplateGroup,
            getClass(),
            superGroup);

        if(this.literals != null) {
          this.literals.setErrorListener(errorListener);
        }
        this.group.setErrorListener(errorListener);
    }

    private static StringTemplateGroup createGroup(final String templateGroupFilePathOnClasspath,
                                                   final StringTemplateErrorListener errorListener,
                                                   final String cacheKey,
                                                   final boolean allowImplicitTemplateGroup,
                                                   final Class<?> clazz,
                                                   final StringTemplateGroup superGroup)
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
                result = new StringTemplateGroup(reader, errorListener);
            }
            else {
                result = new StringTemplateGroup(reader, null, errorListener, superGroup);
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

    @Override
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
            final String key = Base64.getEncoder().encodeToString(name.getBytes(UTF_8));
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

    public static class Builder
    {
        private final String basePath;
        private String superGroupPath;
        private StringTemplateErrorListener errorListener = null;
        private boolean allowImplicitTemplateGroupEnabled = false;
        private boolean treatLiteralsAsTemplatesEnabled = false;
        private boolean shouldCacheEnabled = false;

        Builder(final Class<?> baseClass)
        {
            this.basePath = mungify(baseClass);
        }

        Builder(final String basePath)
        {
            this.basePath = basePath;
        }

        public Builder withSuperGroup(final Class<?> superGroupClass)
        {
            this.superGroupPath = mungify(superGroupClass);
            return this;
        }

        public Builder withSuperGroup(final String superGroupPath)
        {
            this.superGroupPath = superGroupPath;
            return this;
        }

        public Builder withErrorListener(final StringTemplateErrorListener errorListener)
        {
            this.errorListener = errorListener;
            return this;
        }

        public Builder allowImplicitTemplateGroup()
        {
            this.allowImplicitTemplateGroupEnabled = true;
            return this;
        }

        public Builder treatLiteralsAsTemplates()
        {
            this.treatLiteralsAsTemplatesEnabled = true;
            return this;
        }

        public Builder shouldCache()
        {
            this.shouldCacheEnabled = true;
            return this;
        }

        public StringTemplate3StatementLocator build()
        {
            return new StringTemplate3StatementLocator(basePath,
                                                       superGroupPath,
                                                       errorListener,
                                                       allowImplicitTemplateGroupEnabled,
                                                       treatLiteralsAsTemplatesEnabled,
                                                       shouldCacheEnabled);
        }
    }
}
