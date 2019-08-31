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
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.antlr.v4.runtime.CharStreams;
import org.jdbi.v3.core.internal.SqlScriptParser;
import org.jdbi.v3.core.internal.exceptions.CheckedFunction;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.core.locator.internal.ClasspathBuilder;

/**
 * Locates SQL in {@code .sql} files on the classpath.  Given a class and
 * method name, for example {@code com.foo.Bar#query}, load a
 * classpath resource name like {@code com/foo/Bar/query.sql}.
 * The contents are then parsed, cached, and returned for use by a statement.
 */
public final class ClasspathSqlLocator {
    private static final String SQL_EXTENSION = "sql";

    private final Map<ClassLoader, Map<String, String>> cache =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final Function<InputStream, String> parser;

    private ClasspathSqlLocator(CheckedFunction<InputStream, String> parser) {
        this.parser = Unchecked.function(parser);
    }

    /**
     * Locates SQL for the given type and name. Example: Given a type <code>com.foo.Bar</code> and a name of
     * <code>baz</code>, looks for a resource named <code>com/foo/Bar/baz.sql</code> on the classpath and returns its
     * contents as a String.
     *
     * @param type the type that "owns" the given SQL. Dictates the directory path to the SQL resource file on the
     *             classpath.
     * @param methodName the SQL statement name (usually a method or field name from the type).
     * @return the located SQL.
     * @deprecated {@link #create()} an instance instead of using static methods
     */
    @Deprecated
    public static String findSqlOnClasspath(Class<?> type, String methodName) {
        return Holder.INSTANCE.locate(type, methodName);
    }

    /**
     * Locates SQL for the given fully-qualified name. Example: Given the name <code>com.foo.Bar.baz</code>, looks for
     * a resource named <code>com/foo/Bar/baz.sql</code> on the classpath and returns its contents as a String.
     *
     * @param name fully qualified name.
     * @return the located SQL.
     * @deprecated {@link #create()} an instance instead of using static methods
     */
    @Deprecated
    public static String findSqlOnClasspath(String name) {
        return Holder.INSTANCE.locate(name);
    }

    /**
     * Returns resource's contents as a string at the specified path. The path should point directly
     * to the resource at the classpath. The resource is loaded by the current thread's classloader.
     *
     * @param path the resource path
     * @return the resource's contents
     * @see ClassLoader#getResource(String)
     * @deprecated {@link #create()} an instance instead of using static methods
     */
    @Deprecated
    public static String getResourceOnClasspath(String path) {
        return Holder.INSTANCE.getResource(path);
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
     * @deprecated {@link #create()} an instance instead of using static methods
     */
    @Deprecated
    public static String getResourceOnClasspath(ClassLoader classLoader, String path) {
        return Holder.INSTANCE.getResource(classLoader, path);
    }

    /**
     * @return a new ClasspathSqlLocator that returns SQL with comments removed
     */
    public static ClasspathSqlLocator removingComments() {
        final SqlScriptParser commentStripper =
                new SqlScriptParser((t, sb) -> sb.append(t.getText()));
        return new ClasspathSqlLocator(
                r -> commentStripper.parse(CharStreams.fromStream(r)));
    }

    /**
     * @return a new ClasspathSqlLocator that returns SQL without modifying it
     */
    public static ClasspathSqlLocator create() {
        return new ClasspathSqlLocator(ClasspathSqlLocator::readAsString);
    }

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
    public String locate(Class<?> type, String methodName) {
        return getResource(
                type.getClassLoader(),
                new ClasspathBuilder()
                    .appendFullyQualifiedClassName(type)
                    .appendVerbatim(methodName)
                    .setExtension(SQL_EXTENSION)
                    .build());
    }

    /**
     * Locates SQL for the given fully-qualified name. Example: Given the name <code>com.foo.Bar.baz</code>, looks for
     * a resource named <code>com/foo/Bar/baz.sql</code> on the classpath and returns its contents as a String.
     *
     * @param name fully qualified name.
     * @return the located SQL.
     */
    public String locate(String name) {
        return getResource(
                selectClassLoader(),
                new ClasspathBuilder()
                    .appendDotPath(name)
                    .setExtension(SQL_EXTENSION)
                    .build());
    }

    /**
     * Returns resource's contents as a string at the specified path. The path should point directly
     * to the resource at the classpath. The resource is loaded by the current thread's classloader.
     *
     * @param path the resource path
     * @return the resource's contents
     * @see ClassLoader#getResource(String)
     */
    public String getResource(String path) {
        return getResource(selectClassLoader(), path);
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
    public String getResource(ClassLoader classLoader, String path) {
        return cache.computeIfAbsent(classLoader, x -> new ConcurrentHashMap<>())
                    .computeIfAbsent(path, x -> readResource(classLoader, path));
    }

    private String readResource(ClassLoader classLoader, String path) {
        try (InputStream is = openStream(classLoader, path)) {
            return parser.apply(is);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read classpath resource at " + path, e);
        }
    }

    private static String readAsString(InputStream is) throws IOException {
        try (Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[256];
            int n;
            while (-1 != (n = r.read(buffer))) { // NOPMD
                result.append(buffer, 0, n);
            }
            return result.toString();
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

    static class Holder {
        static final ClasspathSqlLocator INSTANCE = ClasspathSqlLocator.removingComments();
    }
}
