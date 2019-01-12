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
package org.jdbi.v3.core.locator;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import org.antlr.runtime.ANTLRInputStream;
import org.jdbi.v3.core.internal.SqlScriptParser;
import org.jdbi.v3.core.locator.internal.ClasspathBuilder;

/**
 * Locates SQL in {@code .sql} files on the classpath.  Given a class and
 * method name, for example {@code com.foo.Bar#query}, load a
 * classpath resource name like {@code com/foo/Bar/query.sql}.
 * The contents are then parsed, cached, and returned for use by a statement.
 */
public final class ClasspathSqlLocator {
    private static final SqlScriptParser SQL_SCRIPT_PARSER = new SqlScriptParser((t, sb) -> sb.append(t.getText()));

    @SuppressWarnings("unchecked")
    private static final Map<Entry<ClassLoader, String>, String> CACHE = ExpiringMap.builder()
            .expiration(10, TimeUnit.MINUTES)
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .entryLoader(obj -> {
                Entry<ClassLoader, String> entry = (Entry<ClassLoader, String>) obj;
                return readResource(entry.getKey(), entry.getValue());
            })
            .build();

    private static final String SQL_EXTENSION = "sql";

    private ClasspathSqlLocator() {}

    /**
     * Locates SQL for the given type and name. Example: Given a type <code>com.foo.Bar</code> and a name of
     * <code>baz</code>, looks for a resource named <code>com/foo/Bar/baz.sql</code> on the classpath and returns its
     * contents as a String.
     *
     * @param type the type that "owns" the given SQL. Dictates the directory path to the SQL resource file on the
     *             classpath.
     * @param methodName the SQL statement name (usually a method or field name from the type).
     * @return the located SQL.
     */
    public static String findSqlOnClasspath(Class<?> type, String methodName) {
        String path = new ClasspathBuilder()
            .appendFullyQualifiedClassName(type)
            .appendVerbatim(methodName)
            .setExtension(SQL_EXTENSION)
            .build();

        return getResourceOnClasspath(type.getClassLoader(), path);
    }

    /**
     * Locates SQL for the given fully-qualified name. Example: Given the name <code>com.foo.Bar.baz</code>, looks for
     * a resource named <code>com/foo/Bar/baz.sql</code> on the classpath and returns its contents as a String.
     *
     * @param name fully qualified name.
     * @return the located SQL.
     */
    public static String findSqlOnClasspath(String name) {
        ClasspathBuilder builder = new ClasspathBuilder()
            .appendDotPath(name)
            .setExtension(SQL_EXTENSION);

        return getResourceOnClasspath(selectClassLoader(), builder.build());
    }

    /**
     * Returns resource's contents as a string at the specified path. The path should point directly
     * to the resource at the classpath. The resource is loaded by the current thread's classloader.
     *
     * @param path the resource path
     * @return the resource's contents
     * @see ClassLoader#getResource(String)
     */
    public static String getResourceOnClasspath(String path) {
        return getResourceOnClasspath(selectClassLoader(), path);
    }

    /**
     * Returns resource's contents as a string at the specified path by the specified classloader.
     * The path should point directly to the resource at the classpath. The classloader should have
     * access to the resource.
     *
     * @param classLoader the classloader which loads the resource
     * @param path the resource path
     * @return the resource's contents
     * @see ClassLoader#getResource(String)
     */
    public static String getResourceOnClasspath(ClassLoader classLoader, String path) {
        return CACHE.get(new AbstractMap.SimpleEntry<>(classLoader, path));
    }

    private static String readResource(ClassLoader classLoader, String path) {
        try (InputStream is = openStream(classLoader, path)) {
            // strips away comments
            return SQL_SCRIPT_PARSER.parse(new ANTLRInputStream(is));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read classpath resource at " + path, e);
        }
    }

    private static InputStream openStream(ClassLoader classLoader, String path) {
        InputStream is = classLoader.getResourceAsStream(path);
        if (is == null) {
            throw new IllegalArgumentException("Cannot find classpath resource at " + path);
        }
        return is;
    }

    private static ClassLoader selectClassLoader() {
        return Optional.ofNullable(Thread.currentThread().getContextClassLoader())
                .orElseGet(ClasspathSqlLocator.class::getClassLoader);
    }
}
