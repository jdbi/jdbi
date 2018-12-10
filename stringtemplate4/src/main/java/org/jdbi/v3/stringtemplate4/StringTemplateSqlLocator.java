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

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jdbi.v3.core.locator.internal.ClasspathBuilder;
import org.jdbi.v3.lib.internal.expiringmap.net.jodah.expiringmap.ExpirationPolicy;
import org.jdbi.v3.lib.internal.expiringmap.net.jodah.expiringmap.ExpiringMap;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

/**
 * Locates SQL in <code>.sql.stg</code> StringTemplate group files on the classpath.
 */
public class StringTemplateSqlLocator {
    private static final Map<String, ThreadLocal<STGroup>> CACHE = ExpiringMap.builder()
            .expiration(10, TimeUnit.MINUTES)
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .build();

    private StringTemplateSqlLocator() {}

    /**
     * Locates SQL for the given type and name. Example: Given a type <code>com.foo.Bar</code> and a name of
     * <code>baz</code>, loads a StringTemplate group file from the resource named <code>com/foo/Bar.sql.stg</code> on
     * the classpath, and returns the template with the given name from the group.
     *
     * @param type the type that "owns" the given StringTemplate group file. Dictates the filename of the
     *             StringTemplate group file on the classpath.
     * @param name the template name within the StringTemplate group.
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
     * Locates SQL for the given type and name. Loads a StringTemplate group from the resource at the given path,
     * and returns the template with the given name from the group.
     *
     * @param path the resource path for the StringTemplate group.
     * @param name the template name within the StringTemplate group.
     * @return the located SQL.
     */
    public static ST findStringTemplate(String path, String name) {
        STGroup group = findStringTemplateGroup(path);

        return findTemplateInGroup(path, name, group);
    }

    /**
     * Locates SQL for the given type and name. Loads a StringTemplate group from the resource at the given path,
     * and returns the template with the given name from the group.
     *
     * @param classLoader the classloader from which to load the resource.
     * @param path the resource path for the StringTemplate group.
     * @param name the template name within the StringTemplate group.
     * @return the located SQL.
     */
    public static ST findStringTemplate(ClassLoader classLoader, String path, String name) {
        STGroup group = findStringTemplateGroup(classLoader, path);

        return findTemplateInGroup(path, name, group);
    }

    private static ST findTemplateInGroup(String path, String name, STGroup group) {
        if (!group.isDefined(name)) {
            throw new IllegalStateException("No StringTemplate group " + name + " for path " + path);
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
        String path = new ClasspathBuilder()
            .appendFullyQualifiedClassName(type)
            .setExtension("sql.stg")
            .build();

        return findStringTemplateGroup(type.getClassLoader(), path);
    }

    /**
     * Loads the StringTemplateGroup from the given path on the classpath.
     *
     * @param path the resource path on the classpath.
     * @return the loaded StringTemplateGroup.
     */
    public static STGroup findStringTemplateGroup(String path) {
        return findStringTemplateGroup(Thread.currentThread().getContextClassLoader(), path);
    }

    /**
     * Loads the StringTemplateGroup from the given path on the classpath.
     *
     * @param classLoader the classloader from which to load the resource.
     * @param path the resource path on the classpath.
     * @return the loaded StringTemplateGroup.
     */
    public static STGroup findStringTemplateGroup(ClassLoader classLoader, String path) {
        return CACHE.computeIfAbsent(path, p -> ThreadLocal.withInitial(() -> readStringTemplateGroup(classLoader, path))).get();
    }

    private static STGroup readStringTemplateGroup(ClassLoader classLoader, String path) {
        try {
            URL resource = classLoader.getResource(path);
            STGroupFile group = new STGroupFile(resource, StandardCharsets.UTF_8.name(), '<', '>');
            group.load();
            return group;
        } catch (Exception e) {
            throw new RuntimeException("Unable to read StringTemplate group file at " + path + " on classpath", e);
        }
    }
}
