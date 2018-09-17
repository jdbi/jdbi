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
package org.jdbi.v3.core.locator.internal;

import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ClasspathBuilder {
    private static final String DOT = ".";
    private static final String SLASH = "/";

    // a part can consist of already slash-concatenated parts
    // preserving individual inputs can help see the bigger picture in debugging
    // eagerly splitting them is quite pointless
    private final List<String> parts = new ArrayList<>();
    private String extension = null;

    public ClasspathBuilder setExtension(@Nullable String extension) {
        if (extension != null) {
            String trimmed = extension.trim();
            if (!trimmed.isEmpty()) {
                this.extension = trimmed;
            }
        }
        return this;
    }

    // safe appenders:

    public ClasspathBuilder appendPackage(@Nonnull Package pkg) {
        return appendDotPath(pkg.getName());
    }

    public ClasspathBuilder appendSimpleClassName(@Nonnull Class clazz) {
        return add(clazz.getSimpleName());
    }

    public ClasspathBuilder appendFullyQualifiedClassName(@Nonnull Class clazz) {
        return appendDotPath(clazz.getName());
    }

    // shame we can't get the name out of a MethodHandle
    public ClasspathBuilder appendMethodName(@Nonnull Method method) {
        return add(method.getName());
    }

    public ClasspathBuilder appendSimpleClassAndMethodName(@Nonnull Method method) {
        return appendSimpleClassName(method.getDeclaringClass()).appendMethodName(method);
    }

    public ClasspathBuilder appendFullyQualifiedMethodName(@Nonnull Method method) {
        return appendFullyQualifiedClassName(method.getDeclaringClass()).appendMethodName(method);
    }

    // https://google.com/ -> com/google
    public ClasspathBuilder appendHostname(@Nonnull URI uri) {
        List<String> domain = Arrays.asList(uri.getHost().split(Pattern.quote(DOT)));
        Collections.reverse(domain);
        return add(String.join(SLASH, domain));
    }

    // try to use only relative paths here
    public ClasspathBuilder appendRelativePath(@Nonnull Path path) {
        path.iterator().forEachRemaining(dir -> add(dir.getFileName().toString()));
        return this;
    }

    // unsafe appenders:

    // because sometimes you just don't have fancy data structures to work on
    public ClasspathBuilder appendVerbatim(@Nonnull String s) {
        sanitize(s).ifPresent(this::add);
        return this;
    }

    // com.google.guava -> com/google/guava
    public ClasspathBuilder appendDotPath(@Nonnull String path) {
        return appendVerbatim(path.replace(DOT, SLASH));
    }

    /**
     * @return the current path string
     */
    public String build() {
        if (parts.isEmpty()) {
            throw new IllegalStateException("specify path parts before building the path");
        }

        String ext = extension == null ? "" : DOT + extension;
        return String.join(SLASH, parts) + ext;
    }

    /**
     * @return a readable representation of this builder — NOT the path string!
     */
    @Override
    public String toString() {
        return extension == null
            ? parts.toString()
            : parts.toString() + " + ." + extension;
    }

    private ClasspathBuilder add(String s) {
        parts.add(s);
        return this;
    }

    private static Optional<String> sanitize(String path) {
        // these do not add levels to a path, so ignore them
        if (path == null || path.isEmpty() || SLASH.equals(path)) {
            return Optional.empty();
        }

        // filter out common beauty flaws like double slashes or whitespace edges
        String sanitized = Arrays.stream(path.split(Pattern.quote(SLASH)))
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .collect(Collectors.joining(SLASH));

        return Optional.of(sanitized);
    }
}
