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
package org.jdbi.v3.locator;

import static java.util.Collections.synchronizedMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import org.antlr.runtime.ANTLRInputStream;
import org.jdbi.v3.internal.SqlScriptParser;

/**
 * Locates SQL in <code>.sql</code> files on the classpath.
 */
public final class ClasspathSqlLocator implements SqlLocator {
    private static final char PACKAGE_DELIMITER = '.';
    private static final char PATH_DELIMITER = '/';

    private static final SqlScriptParser SQL_SCRIPT_PARSER = new SqlScriptParser((t, sb) -> sb.append(t.getText()));

    private final Map<String, String> locatedSql = synchronizedMap(new WeakHashMap<>());

    public ClasspathSqlLocator() {
    }

    /**
     * Locates SQL for the given extension type and name. Example: Given an extension type <code>com.foo.Bar</code> and
     * a name of <code>baz</code>, looks for a resource named <code>/com/foo/Bar/baz.sql</code> and returns its
     * contents as a String.
     *
     * @param extensionType the extension type
     * @param name          the SQL statement name (usually a method name of the extension type).
     * @return the located SQL.
     */
    @Override
    public String locate(Class<?> extensionType, String name) {
        String path = getResourcePath(extensionType, name);
        return locateByPath(extensionType.getClassLoader(), path);
    }

    /**
     * Locates SQL for the given fully-qualified SQL name. Example: Given the name <code>com.foo.Bar.baz</code>, looks
     * for a resource named <code>/com/foo/Bar/baz.sql</code> and returns its contents as a String.
     *
     * @param name fully qualified statement name.
     * @return the located SQL.
     */
    public String locate(String name) {
        String path = getResourcePath(name);
        return locateByPath(selectClassLoader(), path);
    }

    private String getResourcePath(Class<?> extensionType, String methodName) {
        return getResourcePath(extensionType.getCanonicalName() + "." + methodName);
    }

    private String getResourcePath(String fullyQualifiedName) {
        return fullyQualifiedName.replace(PACKAGE_DELIMITER, PATH_DELIMITER) + ".sql";
    }

    private String locateByPath(ClassLoader classLoader, String path) {
        return locatedSql.computeIfAbsent(path, p -> readResource(classLoader, p));
    }

    private String readResource(ClassLoader classLoader, String path) {
        try (InputStream is = openStream(classLoader, path)) {
            // strips away comments
            return SQL_SCRIPT_PARSER.parse(new ANTLRInputStream(is));
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to read classpath resource at " + path, e);
        }
    }

    private InputStream openStream(ClassLoader classLoader, String path) {
        InputStream is = classLoader.getResourceAsStream(path);
        if (is == null) {
            throw new IllegalArgumentException("Cannot find classpath resource at " + path);
        }
        return is;
    }

    private ClassLoader selectClassLoader() {
        return Optional.ofNullable(Thread.currentThread().getContextClassLoader())
                .orElseGet(ClasspathSqlLocator.class::getClassLoader);
    }
}
