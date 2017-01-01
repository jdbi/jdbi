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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupString;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Locates SQL in <code>.sql.stg</code> StringTemplate group files on the classpath.
 */
public class StringTemplateSqlLocator {
    private static final Map<Class<?>, STGroup> CACHE = ExpiringMap.builder()
            .expiration(10, TimeUnit.MINUTES)
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .entryLoader(o -> readStringTemplateGroup((Class) o))
            .build();

    private static final String TEMPLATE_GROUP_EXTENSION = ".sql.stg";

    private StringTemplateSqlLocator() {
    }

    /**
     * Locates SQL for the given type and name. Example: Given a type <code>com.foo.Bar</code> and a name of
     * <code>baz</code>, loads a StringTemplate group file from the resource named <code>com/foo/Bar.sql.stg</code> on
     * the classpath, and returns the template with the given name from the group.
     *
     * @param type the type that "owns" the given StringTemplate group file. Dictates the filename of the
     *             StringTemplate group file on the classpath.
     * @param name the template name in the StringTemplate group.
     * @return the located SQL.
     */
    public static ST findStringTemplate(Class<?> type, String name) {
        STGroup group = findStringTemplateGroup(type);

        if (!group.isDefined(name)) {
            throw new IllegalStateException("No StringTemplate group " + name + " for class " + type);
        }

        return group.getInstanceOf(name);
    }

    /**
     * Loads the StringTemplateGroup for the given type. Example: Given a type <code>com.foo.Bar</code>, returns a
     * StringTemplateGroup loaded from the resource named <code>com/foo/Bar.sql.stg</code> on the classpath.
     *
     * @param type the type that "owns" the given StringTemplate group file. Dictates the filename of the
     *             StringTemplate group file on the classpath.
     * @return the loaded StringTemplateGroup.
     */
    public static STGroup findStringTemplateGroup(Class<?> type) {
        return CACHE.get(type);
    }

    private static STGroup readStringTemplateGroup(Class<?> type) {
        String path = resourcePathFor(type);

        try (InputStream is = openStream(type.getClassLoader(), path)) {
            return new STGroupString(toString(is));
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to read StringTemplate group file at " + path + " on classpath", e);
        }
    }

    private static String toString(InputStream inputStream) throws IOException {
        char[] buffer = new char[1024];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(inputStream, UTF_8);
        for (int rsz; (rsz = in.read(buffer, 0, buffer.length)) >= 0;) {
            out.append(buffer, 0, rsz);
        }
        return out.toString();
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
