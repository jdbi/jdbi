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
package org.jdbi.v3.stringtemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.antlr.stringtemplate.StringTemplateGroup;

/**
 * Locates SQL in <code>.sql.stg</code> StringTemplate group files on the classpath.
 */
public class StringTemplateSqlLocator {
    private static final ConcurrentMap<Class<?>, StringTemplateGroup> CACHE = new ConcurrentHashMap<>();
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final String TEMPLATE_GROUP_EXTENSION = ".sql.stg";

    private StringTemplateSqlLocator() {
    }

    /**
     * Locates SQL for the given extension type and name. Example: Given an extension type <code>com.foo.Bar</code> and
     * a name of <code>baz</code>, loads a StringTemplate group file from the resource named
     * <code>com/foo/Bar.sql.stg</code> on the classpath, and returns the template with the given name from the group.
     *
     * @param extensionType the extension type
     * @param name          the template name in the StringTemplate group.
     * @return the located SQL.
     */
    public static String findStringTemplateSql(Class<?> extensionType, String name) {
        StringTemplateGroup group = CACHE.computeIfAbsent(extensionType, StringTemplateSqlLocator::readGroupResource);

        if (!group.isDefined(name)) {
            throw new IllegalStateException("No StringTemplate group " + name + " for class " + extensionType);
        }
        return group.getInstanceOf(name).getTemplate();
    }

    private static StringTemplateGroup readGroupResource(Class<?> sqlObjectType) {
        String path = resourcePathFor(sqlObjectType);

        try (InputStream is = openStream(sqlObjectType.getClassLoader(), path)) {
            InputStreamReader reader = new InputStreamReader(is, UTF_8);
            return new StringTemplateGroup(reader);
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to read StringTemplate group file at " + path + " on classpath", e);
        }
    }

    private static InputStream openStream(ClassLoader classLoader, String path) {
        InputStream is = classLoader.getResourceAsStream(path);

        if (is == null) {
            throw new IllegalStateException("Unable to find StringTemplate group file at " + path + " on classpath");
        }

        return is;
    }

    private static String resourcePathFor(Class<?> clazz) {
        return clazz.getName().replace('.', '/') + TEMPLATE_GROUP_EXTENSION;
    }
}
